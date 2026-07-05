package tong.statmod.dungeon;

import net.minecraft.world.level.block.Block;

/**
 * Mission M6 — Palette de blocs d'un étage (2026-07-05).
 *
 * <p>Abstraction des matériaux utilisés par {@link DungeonArchitect} pour bâtir la forteresse.
 * Deux implémentations :
 * <ul>
 *   <li>{@link FloorPalette} — palette par <b>tier</b> (EARLY/MID/LATE/ABYSS), le fallback.</li>
 *   <li>{@link ThemePalette} — palette par <b>thème/arc</b> (glace, os, nether, corail…), pour que
 *       l'architecture <i>ressemble</i> à son thème et pas juste au tier.</li>
 * </ul>
 *
 * <p>Grâce à cette interface, tout le code de construction de {@link DungeonArchitect} reste
 * inchangé : il appelle {@code p.base()}, {@code p.accent()}, etc. — seule la palette fournie change.
 */
public interface BlockPalette {

    /** Bloc de structure principal (murs, masse). */
    Block base();

    /** Bloc d'accent (bordures, chapiteaux, sol d'estrade). */
    Block accent();

    /** Source de lumière (torche, lanterne, end rod…). */
    Block light();

    /** Pierre du cône underside (sous l'île). */
    Block underside();

    /** Décor principal dispersé (roche, briques décoratives). */
    Block decorPrimary();

    /** Décor secondaire (posé, hauteur 1-2). */
    Block decorSecondary();

    /** Mur bas (fence wall). */
    Block wallBlock();

    /** Escalier assorti. */
    Block stair();

    /** Dalle assortie. */
    Block slab();

    /** Plafond des pièces fermées. */
    Block ceiling();

    /** Bloc « cicatrice » au sol (dressing narratif : gravats/brûlé). */
    Block scar();

    /** Laine/bloc de bannière déchirée (dressing narratif). */
    Block banner();
}
