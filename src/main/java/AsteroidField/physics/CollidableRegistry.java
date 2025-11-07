package AsteroidField.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.Node;

public final class CollidableRegistry {
    private final List<Node> nodes = new ArrayList<>();
    public List<Node> view() { return Collections.unmodifiableList(nodes); }
    public void add(Node n) { if (n != null) nodes.add(n); }
    public void addAll(List<? extends Node> ns) { if (ns != null) nodes.addAll(ns); }
    public void remove(Node n) { nodes.remove(n); }
    public void clear() { nodes.clear(); }
}
