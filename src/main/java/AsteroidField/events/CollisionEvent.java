package AsteroidField.events;

import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Point3D;
import javafx.scene.Node;

/** Fired when something collides (ship, tether, etc.). World (parent) space positions/normals. */
public final class CollisionEvent extends GameEvent {

    public static final EventType<CollisionEvent> ANY            = new EventType<>(GameEvent.ANY, "COLLISION");
    public static final EventType<CollisionEvent> SHIP_COLLISION = new EventType<>(ANY, "SHIP_COLLISION");
    public static final EventType<CollisionEvent> TETHER_ATTACH  = new EventType<>(ANY, "TETHER_ATTACH");

    private final Point3D positionWorld;
    private final Point3D normalWorld;
    private final double relativeSpeed; // along the normal at impact (>= 0)
    private final Node collider;        // the node we hit (optional)

    public CollisionEvent(EventType<? extends CollisionEvent> type,
                          Point3D positionWorld,
                          Point3D normalWorld,
                          double relativeSpeed,
                          Node collider) {
        super(type);
        this.positionWorld = positionWorld;
        this.normalWorld = normalWorld;
        this.relativeSpeed = relativeSpeed;
        this.collider = collider;
    }

    public CollisionEvent(Object source, EventTarget target, EventType<? extends CollisionEvent> type,
                          Point3D positionWorld, Point3D normalWorld, double relativeSpeed, Node collider) {
        super(source, target, type);
        this.positionWorld = positionWorld;
        this.normalWorld = normalWorld;
        this.relativeSpeed = relativeSpeed;
        this.collider = collider;
    }

    public Point3D getPositionWorld() { return positionWorld; }
    public Point3D getNormalWorld()   { return normalWorld; }
    public double  getRelativeSpeed() { return relativeSpeed; }
    public Node    getCollider()      { return collider; }
}
