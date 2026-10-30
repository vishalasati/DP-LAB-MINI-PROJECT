package college;

import college.controller.CollegeController;
import college.model.CollegeModel;
import college.view.ConsoleView;

public class Main {
    public static void main(String[] args) {
        CollegeModel model = new CollegeModel();
        ConsoleView view = new ConsoleView();
        CollegeController controller = new CollegeController(model, view);

        if (args.length > 0 && args[0].equals("--demo")) {
            controller.runDemo();
        } else {
            controller.run();
        }
    }
}
