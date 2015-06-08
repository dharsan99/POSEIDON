package uk.ac.ox.oxfish.gui.widget;

import org.metawidget.swing.SwingMetawidget;
import uk.ac.ox.oxfish.gui.MetaInspector;
import uk.ac.ox.oxfish.model.scenario.Scenario;
import uk.ac.ox.oxfish.model.scenario.Scenarios;
import uk.ac.ox.oxfish.utility.StrategyFactories;
import uk.ac.ox.oxfish.utility.StrategyFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A panel containing a combo-box to select a strategy factory
 * Created by carrknight on 6/7/15.
 */
public class StrategyFactoryDialog<T> extends JPanel implements ActionListener
{

    private final JPanel settings;
    /**
     * what is selected
     */
    private StrategyFactory selected;


    /**
     * a map of the constructors instanced (unlike the original constructor map which has factories)
     */
    private final Map<String,StrategyFactory> instancedConstructorMap;

    /**
     * what strategy are we dealing with
     * @param strategyClass the strategy superclass
     */
    public StrategyFactoryDialog(Class strategyClass)
    {
        LinkedList<JRadioButton> buttons = new LinkedList<>();

        //first see if it's a super-class, that makes it easy
        Map<String, ? extends Supplier<? extends StrategyFactory<?>>> constructorMap
                = StrategyFactories.CONSTRUCTOR_MAP.get(strategyClass);
        instancedConstructorMap = new HashMap<>();
        this.setLayout(new BorderLayout());

        //initially empty settings panel
        settings = new JPanel(new CardLayout());
        this.add(new JScrollPane(settings),BorderLayout.CENTER);

        //create radio buttons on the left
        JPanel factories = new JPanel(new GridLayout(0,1));
        this.add(factories,BorderLayout.WEST);
        ButtonGroup factoryGroup  = new ButtonGroup();
        for(Map.Entry<String, ? extends Supplier<? extends StrategyFactory>> factoryItem : constructorMap.entrySet())
        {
            final JRadioButton factoryButton = new JRadioButton(factoryItem.getKey());
            factoryButton.setActionCommand(factoryItem.getKey());
            factories.add(factoryButton);
            buttons.add(factoryButton);
            factoryGroup.add(factoryButton);
            factoryButton.addActionListener(this);

            //create widget
            SwingMetawidget widget = new SwingMetawidget();
            MetaInspector.STANDARD_WIDGET_SETUP(widget, null);
            final StrategyFactory toInspect = factoryItem.getValue().get();
            instancedConstructorMap.put(factoryItem.getKey(),toInspect);
            widget.setToInspect(toInspect);
            settings.add(factoryItem.getKey(),widget);

        }

        factoryGroup.clearSelection();
        //foce first to be selection
        buttons.getFirst().doClick();


    }


    /**
     * Invoked when an action occurs.
     *
     * @param e click
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        //new scenario!
        selected = instancedConstructorMap.get(e.getActionCommand());

        CardLayout cl = (CardLayout)(settings.getLayout());
        cl.show(settings, e.getActionCommand());

        settings.repaint();
        this.repaint();
    }

    public StrategyFactory  getSelected() {
        return selected;
    }
}