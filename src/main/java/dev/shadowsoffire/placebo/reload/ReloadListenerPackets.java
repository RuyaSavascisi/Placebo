package dev.shadowsoffire.placebo.reload;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.datafixers.util.Either;

import dev.shadowsoffire.placebo.Placebo;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.network.MessageHelper;
import dev.shadowsoffire.placebo.network.PayloadProvider;
import dev.shadowsoffire.placebo.reload.DynamicRegistry.SyncManagement;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

@ApiStatus.Internal
public class ReloadListenerPackets {

    public static record Start(String path) implements CustomPacketPayload {

        public static final ResourceLocation ID = Placebo.loc("reload_sync_start");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.path, 50);
        }

        public static class Provider implements PayloadProvider<Start, PlayPayloadContext> {

            @Override
            public ResourceLocation getMsgId() {
                return ID;
            }

            @Override
            public Start read(FriendlyByteBuf buf) {
                return new Start(buf.readUtf(50));
            }

            @Override
            public void handle(Start msg, PlayPayloadContext ctx) {
                MessageHelper.handle(() -> SyncManagement.initSync(msg.path), ctx);
            }

            @Override
            public List<ConnectionProtocol> getSupportedProtocols() {
                return List.of(ConnectionProtocol.PLAY);
            }

            @Override
            public Optional<PacketFlow> getFlow() {
                return Optional.of(PacketFlow.CLIENTBOUND);
            }
        }
    }

    public static record Content<V extends CodecProvider<? super V>>(String path, ResourceLocation key, Either<V, FriendlyByteBuf> data) implements CustomPacketPayload {

        public static final ResourceLocation ID = Placebo.loc("reload_sync_content");

        public Content(String path, ResourceLocation key, V item) {
            this(path, key, Either.left(item));
        }

        private Content(String path, ResourceLocation key, FriendlyByteBuf buf) {
            this(path, key, Either.right(buf));
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.path, 50);
            buf.writeResourceLocation(this.key);
            SyncManagement.writeItem(this.path, this.data.left().get(), buf);
        }

        private V readItem() {
            FriendlyByteBuf buf = this.data.right().get();
            try {
                return SyncManagement.readItem(path, buf);
            }
            catch (Exception ex) {
                Placebo.LOGGER.error("Failure when deserializing a dynamic registry object via network: Registry: {}, Object ID: {}", path, key);
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
            finally {
                buf.release();
            }
        }

        public static class Provider<V extends CodecProvider<? super V>> implements PayloadProvider<Content<V>, PlayPayloadContext> {

            @Override
            public ResourceLocation getMsgId() {
                return ID;
            }

            @Override
            public Content<V> read(FriendlyByteBuf buf) {
                String path = buf.readUtf(50);
                ResourceLocation key = buf.readResourceLocation();
                return new Content<>(path, key, new FriendlyByteBuf(buf.copy()));
            }

            @Override
            public void handle(Content<V> msg, PlayPayloadContext ctx) {
                MessageHelper.handle(() -> SyncManagement.acceptItem(msg.path, msg.key, msg.readItem()), ctx);
            }

            @Override
            public List<ConnectionProtocol> getSupportedProtocols() {
                return List.of(ConnectionProtocol.PLAY);
            }

            @Override
            public Optional<PacketFlow> getFlow() {
                return Optional.of(PacketFlow.CLIENTBOUND);
            }
        }
    }

    public static record End(String path) implements CustomPacketPayload {

        public static final ResourceLocation ID = Placebo.loc("reload_sync_start");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(this.path, 50);
        }

        public static class Provider implements PayloadProvider<End, PlayPayloadContext> {

            @Override
            public ResourceLocation getMsgId() {
                return ID;
            }

            @Override
            public End read(FriendlyByteBuf buf) {
                return new End(buf.readUtf(50));
            }

            @Override
            public void handle(End msg, PlayPayloadContext ctx) {
                MessageHelper.handle(() -> SyncManagement.endSync(msg.path), ctx);
            }

            @Override
            public List<ConnectionProtocol> getSupportedProtocols() {
                return List.of(ConnectionProtocol.PLAY);
            }

            @Override
            public Optional<PacketFlow> getFlow() {
                return Optional.of(PacketFlow.CLIENTBOUND);
            }
        }
    }
}