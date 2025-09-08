package AsteroidField.spacecraft.cockpit;

import javafx.scene.paint.PhongMaterial;

/** Hemispherical icosahedron-derived dome (keeps faces with z >= 0). */
public final class IcosaDomeCockpit extends BaseCockpit {
    public IcosaDomeCockpit(double radius, double strutThickness, boolean addGlass) {
        this(radius, strutThickness, addGlass, null, null);
    }
    public IcosaDomeCockpit(double radius, double strutThickness, boolean addGlass,
                            PhongMaterial frameMaterial, PhongMaterial glassMaterial) {
        super(radius, strutThickness, addGlass, frameMaterial, glassMaterial);
        BaseCockpit.Polyhedron ico = IcosahedronCockpit.icosahedron(radius);
        buildFromPolyhedron(ico, (a,b,c) -> a.getZ() >= 0 && b.getZ() >= 0 && c.getZ() >= 0);
    }
}