package tong.statmod.integration.puffish;

public final class PuffishScreenService {
    private PuffishScreenService() {}

    public static void open(PuffishScreenGateway gateway) {
        gateway.refreshCategories();
        gateway.openScreen();
    }
}
