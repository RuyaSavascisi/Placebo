package shadows.placebo.patreon;

import java.util.Locale;

import org.apache.commons.lang3.text.WordUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import shadows.placebo.Placebo;
import shadows.placebo.patreon.PatreonUtils.PatreonParticleType;
import shadows.placebo.patreon.PatreonUtils.WingType;

@SuppressWarnings("deprecation")
@EventBusSubscriber(value = Dist.CLIENT, modid = Placebo.MODID)
public class PatreonPreview {

	public static final boolean PARTICLES = false;
	public static final boolean WINGS = false;

	private static int counter = 0;

	@SubscribeEvent
	public static void tick(PlayerTickEvent e) {
		if (e.phase == Phase.END && e.player.level().isClientSide) {
			if (e.player.tickCount >= 200) {
				if (e.player.tickCount % 150 == 0) {
					Minecraft mc = Minecraft.getInstance();
					if (PARTICLES) {
						PatreonParticleType[] arr = PatreonParticleType.values();
						PatreonParticleType p = arr[counter++ % arr.length];
						Component type = Component.literal(WordUtils.capitalize(p.name().toLowerCase(Locale.ROOT).replace('_', ' ')));
						mc.gui.setTimes(0, 40, 20);
						mc.gui.setSubtitle(type);
						mc.gui.setTitle(Component.literal(""));
						TrailsManager.TRAILS.put(e.player.getUUID(), p);
					} else if (WINGS) {
						WingType[] arr = WingType.values();
						WingType p = arr[counter++ % arr.length];
						Component type = Component.literal(WordUtils.capitalize(p.name().toLowerCase(Locale.ROOT).replace('_', ' ')));
						mc.gui.setTimes(0, 40, 20);
						mc.gui.setSubtitle(type);
						mc.gui.setTitle(Component.literal(""));
						WingsManager.WINGS.put(e.player.getUUID(), p);
					}
				}
			}
		}
	}

}
