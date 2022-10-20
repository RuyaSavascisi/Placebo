package shadows.placebo.util;

import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Level Function that allows for only returning "nice" stepped numbers.
 */
public class StepFunction implements Float2FloatFunction {

	protected final float min;
	protected final int steps;
	protected final float step;

	/**
	 * Create a new StepFunction 
	 * @param min The min value
	 * @param steps The max number of steps
	 * @param step The value per step
	 */
	public StepFunction(float min, int steps, float step) {
		this.min = min;
		this.steps = steps;
		this.step = step;
		Preconditions.checkArgument(steps > 0);
	}

	@Override
	public float get(float level) {
		return min + (int) (steps * (level + 0.5F / steps)) * step;
	}

	public int getInt(float level) {
		return (int) get(level);
	}

	public float min() {
		return this.min;
	}

	public float max() {
		return this.min + this.steps * step;
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeFloat(this.min);
		buf.writeInt(this.steps);
		buf.writeFloat(this.step);
	}

	public static StepFunction read(FriendlyByteBuf buf) {
		return new StepFunction(buf.readFloat(), buf.readInt(), buf.readFloat());
	}

	public static StepFunction constant(float val) {
		return new StepFunction(val, 1, 0);
	}

}