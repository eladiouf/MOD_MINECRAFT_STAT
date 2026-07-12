package tong.statmod.integration.sdm;

/**
 * État client du pontage PNJ → SDM Shop.
 * <ul>
 *   <li>{@link #targetTab} : onglet à sélectionner à la prochaine ouverture du shop
 *       (consommé par les mixins ShopPage/ShopPageModern).</li>
 *   <li>{@link #lockedTab} : tant que non-nul, le shop est VERROUILLÉ sur cet onglet —
 *       {@code TabPanelMixin} ne construit que son bouton, les autres rayons sont
 *       invisibles. Posé au clic sur un PNJ marchand spécialisé, levé dès que le shop
 *       est ouvert autrement (commande, PNJ généraliste).</li>
 * </ul>
 */
public final class ClientShopTabForcer {
    public static String targetTab = null;
    public static String lockedTab = null;
}
