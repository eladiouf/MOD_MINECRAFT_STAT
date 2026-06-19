package commonnetwork.api;

import commonnetwork.networking.PacketRegistrar;
import commonnetwork.networking.data.PacketContext;
import commonnetwork.networking.data.Side;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Network {
    private static final Logger LOGGER = LoggerFactory.getLogger("commonnetwork-compat");
    private static final List<Registration<?>> REGISTRATIONS = new ArrayList<>();
    private static final Map<Class<?>, Registration<?>> REGISTRATIONS_BY_CLASS = new LinkedHashMap<>();
    private static final NetworkHandler HANDLER = new CompatNetworkHandler();

    private Network() {}

    public static PacketRegistrar registerPacket(CustomPacketPayload.Type<?> type,
                                                 Class<?> messageClass,
                                                 StreamCodec<? super FriendlyByteBuf, ?> codec,
                                                 Consumer<?> consumer) {
        Registration<?> registration = new Registration<>(
                type,
                messageClass,
                codec,
                consumer,
                packetDirection(messageClass.getName()));
        REGISTRATIONS_BY_CLASS.put(messageClass, registration);
        REGISTRATIONS.add(registration);
        return PacketRegistrar.INSTANCE;
    }

    public static NetworkHandler getNetworkHandler() {
        return HANDLER;
    }

    public static void registerCompatPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("commonnetwork-compat");
        for (Registration<?> registration : REGISTRATIONS) {
            registration.register(registrar);
        }
    }

    static PacketDirection packetDirection(String className) {
        if (className.contains(".packets.S2C.")) {
            return PacketDirection.CLIENTBOUND;
        }
        if (className.contains(".packets.C2S.")) {
            return PacketDirection.SERVERBOUND;
        }
        return PacketDirection.BIDIRECTIONAL;
    }

    static Side packetSide(PacketFlow flow) {
        return flow == PacketFlow.CLIENTBOUND ? Side.CLIENT : Side.SERVER;
    }

    @SuppressWarnings("unchecked")
    private static <T> Registration<T> registrationFor(T message) {
        Registration<?> registration = REGISTRATIONS_BY_CLASS.get(message.getClass());
        if (registration == null) {
            throw new IllegalArgumentException("No commonnetwork registration for " + message.getClass().getName());
        }
        return (Registration<T>) registration;
    }

    private static final class CompatNetworkHandler implements NetworkHandler {
        @Override
        public void sendToClient(Object message, ServerPlayer player) {
            Registration<Object> registration = registrationFor(message);
            PacketDistributor.sendToPlayer(player, new WrappedPayload<>(registration, message));
        }

        @Override
        public void sendToServer(Object message) {
            Registration<Object> registration = registrationFor(message);
            PacketDistributor.sendToServer(new WrappedPayload<>(registration, message));
        }
    }

    enum PacketDirection {
        CLIENTBOUND,
        SERVERBOUND,
        BIDIRECTIONAL
    }

    private record WrappedPayload<T>(Registration<T> registration, T message) implements CustomPacketPayload {
        @Override
        @SuppressWarnings("unchecked")
        public Type<? extends CustomPacketPayload> type() {
            return (Type<? extends CustomPacketPayload>) registration.type();
        }
    }

    private record Registration<T>(CustomPacketPayload.Type<?> type,
                                   Class<?> messageClass,
                                   StreamCodec<? super FriendlyByteBuf, ?> codec,
                                   Consumer<?> consumer,
                                   PacketDirection direction) {
        void register(PayloadRegistrar registrar) {
            switch (direction) {
                case CLIENTBOUND -> registrar.playToClient(payloadType(), wrappedCodec(), this::handlePayload);
                case SERVERBOUND -> registrar.playToServer(payloadType(), wrappedCodec(), this::handlePayload);
                case BIDIRECTIONAL -> registrar.playBidirectional(payloadType(), wrappedCodec(), this::handlePayload);
            }
        }

        @SuppressWarnings("unchecked")
        private CustomPacketPayload.Type<WrappedPayload<T>> payloadType() {
            return (CustomPacketPayload.Type<WrappedPayload<T>>) type;
        }

        @SuppressWarnings("unchecked")
        private StreamCodec<RegistryFriendlyByteBuf, WrappedPayload<T>> wrappedCodec() {
            StreamCodec<FriendlyByteBuf, T> messageCodec = (StreamCodec<FriendlyByteBuf, T>) codec;
            return new StreamCodec<>() {
                @Override
                public WrappedPayload<T> decode(RegistryFriendlyByteBuf buffer) {
                    return new WrappedPayload<>(Registration.this, messageCodec.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, WrappedPayload<T> payload) {
                    messageCodec.encode(buffer, payload.message());
                }
            };
        }

        @SuppressWarnings("unchecked")
        private void handlePayload(WrappedPayload<T> payload, IPayloadContext context) {
            Consumer<PacketContext<T>> packetConsumer = (Consumer<PacketContext<T>>) consumer;
            ServerPlayer sender = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            PacketContext<T> packetContext = new PacketContext<>(payload.message(), packetSide(context.flow()), sender);
            context.enqueueWork(() -> packetConsumer.accept(packetContext))
                    .exceptionally(throwable -> {
                        LOGGER.error("Failed to handle commonnetwork packet {}", messageClass.getName(), throwable);
                        return null;
                    });
        }
    }
}
