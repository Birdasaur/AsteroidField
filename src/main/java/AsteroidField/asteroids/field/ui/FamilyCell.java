package AsteroidField.asteroids.field.ui;

import AsteroidField.asteroids.field.families.WeightedFamilyEntry;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

/** ListView cell: checkbox + weight slider for each family. */
public final class FamilyCell extends ListCell<WeightedFamilyEntry> {
    @Override protected void updateItem(WeightedFamilyEntry item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) { setGraphic(null); setText(null); return; }

        CheckBox cb = new CheckBox(item.displayName());
        cb.selectedProperty().bindBidirectional(item.enabledProperty());

        Slider w = new Slider(0, 5, item.getWeight());
        w.setPrefWidth(120);
        w.valueProperty().bindBidirectional(item.weightProperty());

        Label wl = new Label();
        wl.textProperty().bind(item.weightProperty().asString("%.2f"));

        HBox row = new HBox(8, cb, new Label("Weight"), w, wl);
        row.setAlignment(Pos.CENTER_LEFT);
        setGraphic(row);
    }
}
