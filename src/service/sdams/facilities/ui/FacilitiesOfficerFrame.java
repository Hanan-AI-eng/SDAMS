package sdams.facilities.ui;

import java.awt.Component;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import sdams.facilities.service.EquipmentService;
import sdams.facilities.service.FacilityProblemService;
import sdams.facilities.service.StockReportService;
import sdams.facilities.storage.EquipmentRepository;
import sdams.facilities.storage.FacilityProblemRepository;

/**
 * The Facilities Officer's main window: one tab for each of the three functions.
 * The group's login/main menu only needs this line:
 *      FacilitiesOfficerFrame.createWithDefaultFiles().setVisible(true);
 */
public class FacilitiesOfficerFrame extends JFrame {

    public static final String EQUIPMENT_FILE = "data/equipment.txt";
    public static final String PROBLEMS_FILE = "data/facility_problems.txt";

    public FacilitiesOfficerFrame(EquipmentService equipmentService,
                                  FacilityProblemService problemService,
                                  StockReportService stockService) {
        super("SDAMS - Facilities Officer");

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Equipment Inventory", new EquipmentPanel(equipmentService));
        tabs.addTab("Facility Problems", new FacilityProblemPanel(problemService));
        tabs.addTab("Stock Reports", new StockReportPanel(stockService));

        // Every time the user opens a tab, reload it so it shows the latest saved data.
        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            if (selected instanceof Refreshable) {
                ((Refreshable) selected).refresh();
            }
        });

        add(tabs);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // closes only this window, not the whole program
        setSize(1050, 640);
        setLocationRelativeTo(null);
    }

    /** Builds the window using the default text files in the "data" folder. */
    public static FacilitiesOfficerFrame createWithDefaultFiles() {
        EquipmentService equipmentService = new EquipmentService(new EquipmentRepository(EQUIPMENT_FILE));
        FacilityProblemService problemService =
                new FacilityProblemService(new FacilityProblemRepository(PROBLEMS_FILE));
        StockReportService stockService = new StockReportService(equipmentService);
        return new FacilitiesOfficerFrame(equipmentService, problemService, stockService);
    }
}
