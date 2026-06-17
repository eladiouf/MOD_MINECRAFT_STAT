package tong.statmod.integration.puffish;

public interface PuffishMirrorGateway {
    void ensureCategoryUnlocked(String categoryId);

    void setPoints(String categoryId, int points);

    void unlock(String categoryId, String skillId);

    void lock(String categoryId, String skillId);
}
