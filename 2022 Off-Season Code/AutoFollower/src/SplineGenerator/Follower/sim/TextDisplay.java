package SplineGenerator.Follower.sim;

import SplineGenerator.GUI.DisplayGraphics;
import SplineGenerator.GUI.Displayable;
import main.Vector2D;

import java.util.function.Supplier;

public class TextDisplay implements Displayable {
    private Supplier<DisplayData> displayDataSupplier;
    private Vector2D headPosition;
    private int lineSpacing;
    public TextDisplay(Supplier<DisplayData> displayDataSupplier, Vector2D headPosition, int lineSpacing){
        this.displayDataSupplier = displayDataSupplier;
        this.headPosition = headPosition;
        this.lineSpacing = lineSpacing;
    }
    @Override
    public void display(DisplayGraphics graphics) {
        DisplayData displayData = displayDataSupplier.get();
        graphics.getGraphics().drawString("Position: " + displayData.pos.toString(), (int) headPosition.getX(), (int) headPosition.getY());
        graphics.getGraphics().drawString("T: " + displayData.t+"", (int) headPosition.getX(), (int) headPosition.getY()+lineSpacing);
        graphics.getGraphics().drawString("Angle: "+ displayData.angle+"", (int) headPosition.getX(), (int) headPosition.getY()+2*lineSpacing);
        for (int i = 0; i < displayData.waypoints.getWaypoints().size(); i++) {
            graphics.getGraphics().drawString("Waypoint ("+displayData.waypoints.getWaypoints().get(i).getT()+"): "+ "Speed: " + displayData.waypoints.getWaypoints().get(i).getSpeed() + " Has Run: "
                            +displayData.waypoints.getWaypoints().get(i).hasRun()
                    , (int) headPosition.getX(), (int) headPosition.getY()+(3+ i )*lineSpacing);
        }
    }
}
