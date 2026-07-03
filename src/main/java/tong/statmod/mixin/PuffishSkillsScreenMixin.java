package tong.statmod.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import net.puffish.skillsmod.client.data.ClientSkillScreenData;
import net.puffish.skillsmod.client.gui.SkillsScreen;
import net.puffish.skillsmod.util.Bounds2i;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tong.statmod.integration.puffish.PuffishScreenCentering;

@Pseudo
@Mixin(value = SkillsScreen.class, remap = false)
public abstract class PuffishSkillsScreenMixin extends Screen {
    @Shadow @Final private ClientSkillScreenData data;
    @Shadow private int contentPaddingTop;
    @Shadow private int contentPaddingLeft;
    @Shadow private int contentPaddingRight;
    @Shadow private int contentPaddingBottom;

    protected PuffishSkillsScreenMixin(Component title) {
        super(title);
    }

    @Invoker("applyChangesWithLimits")
    protected abstract void statmod$applyChangesWithLimits(int x, int y, float scale, ClientCategoryData data);

    @Inject(method = "init", at = @At("TAIL"))
    private void statmod$centerCategoriesOnOpen(CallbackInfo ci) {
        this.data.streamCategories().forEach(this::statmod$centerCategory);
    }

    @Unique
    private void statmod$centerCategory(ClientCategoryData category) {
        Bounds2i bounds = category.getConfig().getBounds();
        Vector2i min = bounds.min();
        Vector2i max = bounds.max();
        PuffishScreenCentering.ScreenOffset offset = PuffishScreenCentering.centerOnContent(
                this.width,
                this.height,
                this.contentPaddingLeft,
                this.contentPaddingTop,
                this.contentPaddingRight,
                this.contentPaddingBottom,
                category.getScale(),
                min.x(),
                max.x(),
                min.y(),
                max.y()
        );
        this.statmod$applyChangesWithLimits(offset.x(), offset.y(), category.getScale(), category);
    }
}
