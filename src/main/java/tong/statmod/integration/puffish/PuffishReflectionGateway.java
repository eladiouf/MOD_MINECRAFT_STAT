package tong.statmod.integration.puffish;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import tong.statmod.STATMod;

import java.lang.reflect.Method;
import java.util.Optional;

public final class PuffishReflectionGateway implements PuffishMirrorGateway {
    private static final String SKILLS_API_CLASS = "net.puffish.skillsmod.api.SkillsAPI";
    private static final String POINT_SOURCES_CLASS = "net.puffish.skillsmod.util.PointSources";

    private final ServerPlayer player;

    public PuffishReflectionGateway(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void ensureCategoryUnlocked(String categoryId) {
        invokeCategory(categoryId, "unlock");
    }

    @Override
    public void setPoints(String categoryId, int points) {
        Object category = category(categoryId);
        if (category == null) {
            return;
        }
        try {
            Method silentMethod = findMethod(category.getClass(), "setPointsSilently", 3);
            Object commandSource = commandsPointSource();
            if (silentMethod != null && commandSource != null) {
                silentMethod.invoke(category, player, commandSource, points);
                return;
            }
        } catch (ReflectiveOperationException e) {
            STATMod.LOGGER.warn("Puffish silent points sync failed for {}: {}", categoryId, e.getMessage());
        }
        invokeCategory(categoryId, "setExtraPoints", points);
    }

    @Override
    public void unlock(String categoryId, String skillId) {
        invokeSkill(categoryId, skillId, "unlock");
    }

    @Override
    public void lock(String categoryId, String skillId) {
        invokeSkill(categoryId, skillId, "lock");
    }

    private void invokeCategory(String categoryId, String methodName, Object... args) {
        Object category = category(categoryId);
        if (category == null) {
            return;
        }
        try {
            Method method = findMethod(category.getClass(), methodName, args.length + 1);
            if (method == null) {
                return;
            }
            Object[] invokeArgs = new Object[args.length + 1];
            invokeArgs[0] = player;
            System.arraycopy(args, 0, invokeArgs, 1, args.length);
            method.invoke(category, invokeArgs);
        } catch (ReflectiveOperationException e) {
            STATMod.LOGGER.warn("Puffish category call {} failed for {}: {}", methodName, categoryId, e.getMessage());
        }
    }

    private void invokeSkill(String categoryId, String skillId, String methodName) {
        Object skill = skill(categoryId, skillId);
        if (skill == null) {
            return;
        }
        try {
            Method method = findMethod(skill.getClass(), methodName, 1);
            if (method != null) {
                method.invoke(skill, player);
            }
        } catch (ReflectiveOperationException e) {
            STATMod.LOGGER.warn("Puffish skill call {} failed for {} / {}: {}", methodName, categoryId, skillId, e.getMessage());
        }
    }

    private Object category(String categoryId) {
        try {
            Class<?> skillsApi = Class.forName(SKILLS_API_CLASS);
            Method getCategory = skillsApi.getMethod("getCategory", ResourceLocation.class);
            Optional<?> optional = (Optional<?>) getCategory.invoke(null, ResourceLocation.parse(categoryId));
            return optional.orElse(null);
        } catch (ReflectiveOperationException e) {
            STATMod.LOGGER.warn("Puffish category lookup failed for {}: {}", categoryId, e.getMessage());
            return null;
        }
    }

    private Object skill(String categoryId, String skillId) {
        Object category = category(categoryId);
        if (category == null) {
            return null;
        }
        try {
            Method getSkill = findMethod(category.getClass(), "getSkill", 1);
            if (getSkill == null) {
                return null;
            }
            Optional<?> optional = (Optional<?>) getSkill.invoke(category, skillId);
            return optional.orElse(null);
        } catch (ReflectiveOperationException e) {
            STATMod.LOGGER.warn("Puffish skill lookup failed for {} / {}: {}", categoryId, skillId, e.getMessage());
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    private static Object commandsPointSource() {
        try {
            Class<?> pointSources = Class.forName(POINT_SOURCES_CLASS);
            return pointSources.getField("COMMANDS").get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
