package tong.statmod.integration.sdm;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapping métier de villageois → onglet(s) du SDM Shop (noms du catalogue
 * {@link SDMShopCatalog}). Plusieurs onglets séparés par des virgules = le shop
 * du marchand est verrouillé sur ce rayon multi-onglets.
 * <p>
 * Classe pure (aucune classe Minecraft) pour rester testable en JUnit :
 * le test {@code MerchantTabMappingTest} verrouille la cohérence de chaque
 * onglet référencé ici avec le catalogue réel.
 */
public final class MerchantTabMapping {

    private MerchantTabMapping() {}

    /**
     * Onglets SDM d'un métier de villageois ({@code VillagerProfession.name()}).
     * Chaîne vide = PNJ généraliste (shop complet, onglet par défaut).
     */
    public static String tabsFor(String professionName) {
        return switch (professionName) {
            case "weaponsmith" -> "Armes légères,Armes lourdes,Lances et armes d'hast,"
                    + "Armes de Tensura,Armes uniques et légendaires";
            case "toolsmith" -> "Forge et amélioration,Minerais bruts,Lingots et gemmes,Matériaux avancés";
            case "armorer" -> "Armures classiques,Armures fantastiques,Armures historiques,Armures magiques";
            case "cleric" -> "Potions et soins";
            case "fletcher" -> "Armes à distance";
            case "librarian" -> "Magie et parchemins,Runes et composants magiques";
            case "farmer", "butcher" -> "Nourriture";
            default -> "";
        };
    }

    /** Vrai si chaque onglet listé (séparé par des virgules) existe dans le catalogue. */
    public static boolean allTabsExist(String tabsSpec) {
        if (tabsSpec == null || tabsSpec.isEmpty()) {
            return true; // généraliste : toujours valide
        }
        Set<String> catalog = SDMShopCatalog.tabs().stream()
                .map(SDMShopCatalog.ShopTab::name)
                .collect(Collectors.toSet());
        return Arrays.stream(tabsSpec.split(","))
                .map(String::trim)
                .allMatch(catalog::contains);
    }
}
