package dev.shadowsoffire.placebo.reload;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;

import dev.shadowsoffire.placebo.Placebo;
import dev.shadowsoffire.placebo.json.JsonUtil;
import dev.shadowsoffire.placebo.json.PSerializer;
import dev.shadowsoffire.placebo.json.PSerializer.PSerializable;
import dev.shadowsoffire.placebo.json.SerializerMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * A Dynamic Registry is a reload listener which acts like a registry. Unlike datapack registries, it can reload.
 * <p>
 * To utilize this class, subclass it, and provide the appropriate constructor parameters.<br>
 * Then, create a single static instance of it and keep it around.
 * <p>
 * You will provide your serializers via {@link #registerBuiltinSerializers()}.<br>
 * You will then need to register it via {@link #registerToBus()}.<br>
 * From then on, loading of files, condition checks, network sync, and everything else is automatically handled.
 *
 * @param <R> The base type of objects stored in this registry.
 */
public abstract class DynamicRegistry<R extends TypeKeyed & PSerializable<? super R>> extends SimpleJsonResourceReloadListener {

    /**
     * The default serializer key that is used when subtypes are not enabled.
     */
    public static final ResourceLocation DEFAULT = new ResourceLocation("default");

    protected final Logger logger;
    protected final String path;
    protected final boolean synced;
    protected final boolean subtypes;
    protected final SerializerMap<R> serializers;
    protected final Codec<DynamicHolder<R>> holderCodec;

    /**
     * Internal registry. Immutable when outside of the registration phase.
     * <p>
     * This map is cleared in {@link #beginReload()} and frozen in {@link #onReload()}
     */
    protected Map<ResourceLocation, R> registry = ImmutableMap.of();

    /**
     * Staged data used during the sync process. Discarded when running an integrated server.
     */
    private final Map<ResourceLocation, R> staged = new HashMap<>();

    /**
     * Map of all holders that have ever been requested for this registry.
     */
    private final Map<ResourceLocation, DynamicHolder<? extends R>> holders = new ConcurrentHashMap<>();

    /**
     * List of callbacks attached to this registry.
     * 
     * @see #addCallback(RegistryCallback)
     * @see #removeCallback(RegistryCallback)
     */
    private final Set<RegistryCallback<R>> callbacks = new HashSet<>();

    /**
     * Reference to the reload context, if available.<br>
     * Set when the reload event fires, and good for the reload process.
     */
    private WeakReference<ICondition.IContext> context;

    /**
     * Constructs a new dynamic registry.
     *
     * @param logger   The logger used by this listener for all relevant messages.
     * @param path     The datapack path used by this listener for loading files.
     * @param synced   If this listener will be synced over the network.
     * @param subtypes If this listener supports subtyped objects (and the "type" key on top-level objects).
     * @apiNote After construction, {@link #registerToBus()} must be called during setup.
     */
    public DynamicRegistry(Logger logger, String path, boolean synced, boolean subtypes) {
        super(new GsonBuilder().setLenient().create(), path);
        this.logger = logger;
        this.path = path;
        this.synced = synced;
        this.subtypes = subtypes;
        this.serializers = new SerializerMap<>(path);
        this.registerBuiltinSerializers();
        if (this.serializers.isEmpty()) throw new RuntimeException("Attempted to create a json reload listener for " + path + " with no built-in serializers!");
        this.holderCodec = ResourceLocation.CODEC.xmap(this::holder, DynamicHolder::getId);
    }

    /**
     * Processes all the json entries through the registration chain. That registration chain is as follows:
     * <ol>
     * <li>Empty JSON check: Empty values are discarded with a warning message.</li>
     * <li>Condition check: Values that are conditionally disabled are ignored. A note is logged at the trace level.</li>
     * <li>Deserialization: The serializer is pulled from the 'type' field if subtypes is enabled, or the default serializer is used.</li>
     * <li>Validation: Certain states of the object are checked for sanity.</li>
     * <li>Registration: The item is added to the {@link #registry}.</li>
     * </ol>
     */
    @Override
    protected final void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        this.beginReload();
        objects.forEach((key, ele) -> {
            try {
                if (JsonUtil.checkAndLogEmpty(ele, key, this.path, this.logger) && JsonUtil.checkConditions(ele, key, this.path, this.logger, this.getContext())) {
                    JsonObject obj = ele.getAsJsonObject();
                    R deserialized;
                    if (this.subtypes) {
                        deserialized = this.serializers.read(obj);
                    }
                    else {
                        deserialized = this.serializers.get(DEFAULT).read(obj);
                    }
                    deserialized.setId(key);
                    Preconditions.checkNotNull(deserialized.getId(), "A " + this.path + " with id " + key + " failed to set ID.");
                    Preconditions.checkNotNull(deserialized.getSerializer(), "A " + this.path + " with id " + key + " is not declaring a serializer.");
                    Preconditions.checkNotNull(this.serializers.get(deserialized.getSerializer()), "A " + this.path + " with id " + key + " is declaring an unregistered serializer.");
                    this.register(key, deserialized);
                }
            }
            catch (Exception e) {
                this.logger.error("Failed parsing {} file {}.", this.path, key);
                this.logger.error("Underlying Exception: ", e);
            }
        });
        this.onReload();
    }

    /**
     * Add all default serializers to this reload listener.
     * This should be a series of calls to {@link registerSerializer}
     */
    protected abstract void registerBuiltinSerializers();

    /**
     * Called when this manager begins reloading all items.
     * Should handle clearing internal data caches.
     */
    protected void beginReload() {
        this.callbacks.forEach(l -> l.beginReload(this));
        this.registry = new HashMap<>();
        this.holders.values().forEach(DynamicHolder::unbind);
    }

    /**
     * Called after this manager has finished reloading all items.
     * Should handle any info logging, and data immutability.
     */
    protected void onReload() {
        this.registry = ImmutableMap.copyOf(this.registry);
        this.logger.info("Registered {} {}.", this.registry.size(), this.path);
        this.callbacks.forEach(l -> l.onReload(this));
        this.holders.values().forEach(DynamicHolder::bind);
    }

    /**
     * @return An immutable view of all keys registered for this type.
     */
    public Set<ResourceLocation> getKeys() {
        return this.registry.keySet();
    }

    /**
     * @return An immutable view of all items registered for this type.
     */
    public Collection<R> getValues() {
        return this.registry.values();
    }

    /**
     * @return The item associated with this key, or null.
     */
    @Nullable
    public R getValue(ResourceLocation key) {
        return this.getOrDefault(key, null);
    }

    /**
     * @return The item associated with this key, or the default value.
     */
    public R getOrDefault(ResourceLocation key, R defValue) {
        return this.registry.getOrDefault(key, defValue);
    }

    /**
     * Registers this listener to the event bus as is appropriate.
     * This should be called for ALL listeners from common setup.
     */
    public void registerToBus() {
        if (this.synced) SyncManagement.registerForSync(this);
        MinecraftForge.EVENT_BUS.addListener(this::addReloader);
    }

    /**
     * Creates a {@link DynamicHolder} pointing to a value stored in this reload listener.
     *
     * @param <T> The type of the target value.
     * @param id  The ID of the target value.
     * @return A dynamic registry object pointing to the target value.
     */
    @SuppressWarnings("unchecked")
    public <T extends R> DynamicHolder<T> holder(ResourceLocation id) {
        return (DynamicHolder<T>) this.holders.computeIfAbsent(id, k -> new DynamicHolder<>(this, k));
    }

    /**
     * Gets the {@link DynamicHolder} associated with a particular value.
     * 
     * @see #holder(ResourceLocation)
     */
    @SuppressWarnings("unchecked")
    public <T extends R> DynamicHolder<T> holder(T t) {
        return (DynamicHolder<T>) this.holders.computeIfAbsent(t.getId(), k -> new DynamicHolder<>(this, k));
    }

    /**
     * Gets an empty {@link DynamicHolder}.
     * 
     * @see #holder(ResourceLocation)
     */
    @SuppressWarnings("unchecked")
    public DynamicHolder<R> emptyHolder() {
        return (DynamicHolder<R>) this.holders.computeIfAbsent(DynamicHolder.EMPTY, k -> new DynamicHolder<>(this, k));
    }

    /**
     * Returns a {@link Codec} that can handle {@link DynamicHolder}s for this registry.<br>
     * The serialized form is {@link ResourceLocation}.
     * 
     * @return The Dynamic Holder Codec for this registry.
     */
    public Codec<DynamicHolder<R>> holderCodec() {
        return this.holderCodec;
    }

    /**
     * Register a serializer to this listener. Does not permit duplicates, and does not permit multiple registration.
     *
     * @param id         The ID of the serializer. If subtypes are not supported, this is ignored, and {@link #DEFAULT} is used.
     * @param serializer The serializer being registered.
     */
    public final void registerSerializer(ResourceLocation id, PSerializer<? extends R> serializer) {
        serializer.validate(false, this.synced);
        if (this.subtypes) {
            if (this.serializers.contains(id)) throw new RuntimeException("Attempted to register a " + this.path + " serializer with id " + id + " but one already exists!");
            this.serializers.register(id, serializer);
        }
        else {
            if (!this.serializers.isEmpty()) throw new RuntimeException("Attempted to register a " + this.path + " serializer with id " + id + " but subtypes are not supported!");
            this.serializers.register(DEFAULT, serializer);
        }
    }

    /**
     * Registers a ListenerCallback to this reload listener.
     */
    public final boolean addCallback(RegistryCallback<R> callback) {
        return this.callbacks.add(callback);
    }

    /**
     * Removes a ListenerCallback from this reload listener.
     * Must be the same instance as one that was previously registered, or an object that implements equals/hashcode.
     */
    public final boolean removeCallback(RegistryCallback<R> callback) {
        return this.callbacks.remove(callback);
    }

    /**
     * Registers a single item of this type to the registry during reload.
     * <p>
     * Override {@link #validateItem(TypeKeyed)} to perform additional validation of registered objects.
     *
     * @param key  The key of the object being registered.
     * @param item The object being registered.
     * @throws UnsupportedOperationException if the key does not match {@link TypeKeyed#getId() the item's key}
     * @throws UnsupportedOperationException if the key is already in use.
     */
    protected final void register(ResourceLocation key, R item) {
        if (!key.equals(item.getId())) throw new UnsupportedOperationException("Attempted to register a " + this.path + " with a mismatched registry ID! Expected: " + item.getId() + " Provided: " + key);
        if (this.registry.containsKey(key)) throw new UnsupportedOperationException("Attempted to register a " + this.path + " with a duplicate registry ID! Key: " + key);
        this.validateItem(item);
        this.registry.put(key, item);
        this.holders.computeIfAbsent(key, k -> new DynamicHolder<>(this, k));
    }

    /**
     * Validates that an individual item meets any criteria set by this reload listener.<br>
     * Called just before insertion into the registry.
     *
     * @param item The item about to be registered.
     */
    protected void validateItem(R item) {}

    /**
     * @return The context object held in this listener, or {@link IContext.EMPTY} if it is unavailable.
     */
    protected final ICondition.IContext getContext() {
        return this.context.get() != null ? this.context.get() : IContext.EMPTY;
    }

    /**
     * Adds this reload listener to the {@link ReloadableServerResources}.<br>
     * Also records the {@linkplain ICondition.IContext condition context} for use in deserialization.
     */
    private void addReloader(AddReloadListenerEvent e) {
        e.addListener(this);
        this.context = new WeakReference<>(e.getConditionContext());
    }

    /**
     * Replaces the contents of the live registry with the staging registry.<br>
     * This triggers the full reload process for the client.
     *
     * @implNote Not executed when hosting a singleplayer world, as it would replace the server data.
     */
    private void pushStagedToLive() {
        this.beginReload();
        this.staged.forEach(this::register);
        this.onReload();
    }

    /**
     * Sync event handler. Sends the start packet, a content packet for each item, and then the end packet.
     */
    private void sync(OnDatapackSyncEvent e) {
        ServerPlayer player = e.getPlayer();
        PacketTarget target = player == null ? PacketDistributor.ALL.noArg() : PacketDistributor.PLAYER.with(() -> player);

        Placebo.CHANNEL.send(target, new ReloadListenerPacket.Start(this.path));
        this.registry.forEach((k, v) -> {
            Placebo.CHANNEL.send(target, new ReloadListenerPacket.Content<>(this.path, k, v));
        });
        Placebo.CHANNEL.send(target, new ReloadListenerPacket.End(this.path));
    }

    /**
     * Internal class for sync management.
     */
    @ApiStatus.Internal
    static class SyncManagement {

        private static final Map<String, DynamicRegistry<?>> SYNC_REGISTRY = new LinkedHashMap<>();

        /**
         * Registers a {@link DynamicRegistry} for syncing.
         *
         * @param listener The listener to register.
         * @throws UnsupportedOperationException if the listener is not a synced listener.
         * @throws UnsupportedOperationException if the listener is already registered to the sync registry.
         */
        static void registerForSync(DynamicRegistry<?> listener) {
            if (!listener.synced) throw new UnsupportedOperationException("Attempted to register the non-synced JSON Reload Listener " + listener.path + " as a synced listener!");
            synchronized (SYNC_REGISTRY) {
                if (SYNC_REGISTRY.containsKey(listener.path)) throw new UnsupportedOperationException("Attempted to register the JSON Reload Listener for syncing " + listener.path + " but one already exists!");
                if (SYNC_REGISTRY.isEmpty()) MinecraftForge.EVENT_BUS.addListener(SyncManagement::syncAll);
                SYNC_REGISTRY.put(listener.path, listener);
            }
        }

        /**
         * Begins the sync for a specific listener.
         *
         * @param path The path of the listener being synced.
         */
        static void initSync(String path) {
            ifPresent(path, (k, v) -> {
                v.staged.clear();
            });
            Placebo.LOGGER.info("Starting sync for {}", path);
        }

        /**
         * Write an item (with the same type as the listener) to the network.
         *
         * @param <V>   The type of item being written.
         * @param path  The path of the listener.
         * @param value The value being written.
         * @param buf   The buffer being written to.
         */
        @SuppressWarnings("unchecked")
        static <V extends TypeKeyed & PSerializable<? super V>> void writeItem(String path, V value, FriendlyByteBuf buf) {
            ifPresent(path, (k, v) -> {
                ((SerializerMap<V>) v.serializers).write(value, buf);
            });
        }

        /**
         * Reads an item from the network, via the listener's serializers.
         *
         * @param <V>  The type of item being read.
         * @param path The path of the listener.
         * @param key  The key of the item being read (not the serializer key).
         * @param buf  The buffer being read from.
         * @return An object of type V as deserialized from the network.
         */
        @SuppressWarnings("unchecked")
        static <V extends TypeKeyed> V readItem(String path, ResourceLocation key, FriendlyByteBuf buf) {
            var listener = SYNC_REGISTRY.get(path);
            if (listener == null) throw new RuntimeException("Received sync packet for unknown registry!");
            V v = (V) listener.serializers.read(buf);
            v.setId(key);
            return v;
        }

        /**
         * Stages an item to a listener.
         *
         * @param <V>   The type of the item being staged.
         * @param path  The path of the listener.
         * @param value The object being staged.
         */
        @SuppressWarnings("unchecked")
        static <V extends TypeKeyed> void acceptItem(String path, V value) {
            ifPresent(path, (k, v) -> {
                ((Map<ResourceLocation, V>) v.staged).put(value.getId(), value);
            });
        }

        /**
         * Ends the sync for a specific listener.
         * This will delete current data, push staged data to live, and call the appropriate methods for reloading.
         *
         * @param path The path of the listener.
         * @implNote Only called on the logical client.
         */
        static void endSync(String path) {
            if (ServerLifecycleHooks.getCurrentServer() != null) return; // Do not propgate received changed on the host of a singleplayer world, as they may not be the full data.
            ifPresent(path, (k, v) -> {
                v.pushStagedToLive();
            });
        }

        /**
         * Executes an action if the specified path is present in the sync registry.
         */
        private static void ifPresent(String path, BiConsumer<String, DynamicRegistry<?>> consumer) {
            DynamicRegistry<?> value = SYNC_REGISTRY.get(path);
            if (value != null) consumer.accept(path, value);
        }

        private static void syncAll(OnDatapackSyncEvent e) {
            SYNC_REGISTRY.values().forEach(r -> r.sync(e));
        }
    }

}
