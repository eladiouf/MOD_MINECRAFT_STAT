package tong.statmod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WelcomeBookItem extends Item {
    private static final Component[] PAGES = {
        Component.literal("§6§lOWMA TEMP - STAT MOD§r\n\nBienvenue, aventurier !\n\nCe mod ajoute §e23 stats§r à améliorer en jouant.\n\nOuvre l'écran des stats avec §e[P]§r."),
        Component.literal("§6§lLES STATS DE COMBAT§r\n\n§7Force Brute§r: dégâts avec haches/glaives\n§7Technique de Lame§r: dégâts avec épées\n§7Rapidité§r: vitesse d'attaque\n§7Agilité§r: vitesse de déplacement\n§7Résistance Physique§r: -dégâts subis\n§7Endurance§r: +cœurs, récup fatigue\n§7Précision§r: chance de crit"),
        Component.literal("§6§lMAGIE & SURVIE§r\n\n§5Puissance Arcanique§r: dégâts magiques\n§5Affinités§r: Eau/Terre/Feu/Air\n§5Résistance Magique§r: -dégâts magiques\n§5Mana§r: bouclier anti-magie\n§5Érudition§r: +XP orbes\n\n§2Pistage§r: +luck\n§2Sens Aiguisés§r: esquive, voir mobs"),
        Component.literal("§6§lARTISANAT & MENTAL§r\n\n§6Forge§r: dégâts outils, vitesse minage\n§6Cuisine§r: meilleure nourriture\n§6Alchimie§r: durée potions\n\n§dIntimidation§r: faiblesse sur les mobs\n§dVolonté§r: -durée effets négatifs"),
        Component.literal("§6§lBARRES DE SURVIE§r\n\n§b❄ Soif§r: à gauche. Bois de l'eau !\n§6⚡ Fatigue§r: monte quand tu agis. Sneak ou repose-toi.\n§d✦ Mana§r: absorbe les dégâts magiques.\n\n§8Ouvre les perks avec [O], le debug avec [F8]"),
        Component.literal("§6§lCONSEILS§r\n\n• Les premiers niveaux sont rapides\n• Utilise des armes variées\n• Les mobs scales avec toi\n• Tape §e/dev stats§r pour les commandes\n\n§7Bonne aventure !§r")
    };

    public WelcomeBookItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            player.sendSystemMessage(Component.literal("§6§l╔══════════════════════════════════╗"));
            for (int i = 0; i < PAGES.length; i++) {
                player.sendSystemMessage(Component.literal("§6§l║ §ePage " + (i + 1) + "/" + PAGES.length + " §6§l║"));
                player.sendSystemMessage(PAGES[i]);
                if (i < PAGES.length - 1) {
                    player.sendSystemMessage(Component.literal("§8  --- glisse vers le bas ---"));
                }
            }
            player.sendSystemMessage(Component.literal("§6§l╚══════════════════════════════════╝"));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Guide du STAT MOD").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§8Fais un clic droit pour lire").withStyle(ChatFormatting.DARK_GRAY));
    }
}
