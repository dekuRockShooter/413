import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class Project1 extends JFrame implements ActionListener {
    private TextField txField = new TextField();
    private Panel buttonPanel = new Panel();
    // total 20 buttons on the calculator, numbered from left to right,
    // up to down
    private Button buttons[] = new Button[20];
    //bText[] array contains text on corresponding buttons
    private static final String bText[] = {"7","8","9","+","4","5","6",
        "-","1","2","3","*","0",".","=","/","(",")","C","CE"};
    private final Evaluator evaluator = new Evaluator();

    public static void main(String argv[]) {
        Project1 calc = new Project1();
    }

    public Project1() {
        setLayout(new BorderLayout());
        add(txField, BorderLayout.NORTH);
        txField.setEditable(false);
        add(buttonPanel, BorderLayout.CENTER);
        buttonPanel.setLayout(new GridLayout(5,4));
        //create 20 buttons with corresponding text in bText[] array
        for (int i=0; i<20; i++)
                buttons[i] = new Button(bText[i]);
        for (int i=0; i<20; i++) //add buttons to button panel
            buttonPanel.add(buttons[i]);
        for (int i=0; i<20; i++) //set up buttons to listen for mouse input
            buttons[i].addActionListener(this);
        setTitle("Calculator");
        setSize(400, 400);
        setLocationByPlatform(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    public void actionPerformed(ActionEvent arg0) { // You need to fill in this fuction
        Button button = (Button)arg0.getSource();
        String txtContent = this.txField.getText();
        if (button.getLabel().compareTo("C") == 0) {
            if (txtContent.length() > 0)
                txtContent = txtContent.substring(0, txtContent.length() - 1);
        }
        else if (button.getLabel().compareTo("CE") == 0) {
            txtContent = "";
        }
        else if (button.getLabel().compareTo("=") == 0) {
            txtContent = Integer.toString(this.evaluator.eval(txtContent));
        }
        else if (button.getLabel().compareTo(".") == 0) {
            return;
        }
        else {
            txtContent = txtContent + button.getLabel();
        }
        this.txField.setText(txtContent);
    }
}
