package uk.ac.ox.oxfish.gui;

import org.metawidget.inspector.composite.CompositeInspector;
import org.metawidget.inspector.composite.CompositeInspectorConfig;
import org.metawidget.inspector.propertytype.PropertyTypeInspector;
import org.metawidget.swing.SwingMetawidget;
import org.metawidget.swing.widgetprocessor.binding.beanutils.BeanUtilsBindingProcessor;
import org.metawidget.swing.widgetprocessor.binding.beanutils.BeanUtilsBindingProcessorConfig;
import sim.display.GUIState;
import sim.portrayal.Inspector;
import uk.ac.ox.oxfish.gui.widget.*;

/**
 * A meta-widget implementation of the usual inspector. Makes modifying strategy parameters a lot easier
 * Created by carrknight on 6/7/15.
 */
public class MetaInspector extends Inspector
{

    final private Object toInspect;

    final private SwingMetawidget widget = new SwingMetawidget();

    public MetaInspector(Object toInspect, GUIState gui) {
        this.toInspect = toInspect;

        STANDARD_WIDGET_SETUP(widget, gui);

        setVolatile(true); //volatile by default


        widget.setToInspect(toInspect);
        this.add(widget);

    }


    @Override
    public void updateInspector() {
        widget.setToInspect(toInspect);
    }

    /**
     * Setup standard widget with the usual binders and post-processors
     * @param widget the widget to set up
     * @param toSyncAgainst nullable: a link to the gui-state if we need to sync
     *                      the writing to the model. It is also where we get the randomizer so it
     *                      is important whenever the model has already started
     */
    public static void STANDARD_WIDGET_SETUP(SwingMetawidget widget, GUIState toSyncAgainst) {
        //create inspectors
        CompositeInspectorConfig inspectorConfig = new CompositeInspectorConfig().setInspectors(
                //default for swing
                new PropertyTypeInspector(),
                //adds information for selecting factories
                new StrategyFactoryInspector(),
                //tags already instantiated strategies. Useful only with GUI state
                new StrategyInspector(),
                //adds information on randomizable parameters
                new DoubleParameterInspector()
        );
        widget.setInspector(new CompositeInspector(inspectorConfig));
        //add the processor
        //this makes the binding between ui and model possible
        widget.addWidgetProcessor(new BeanUtilsBindingProcessor(new BeanUtilsBindingProcessorConfig()));
        //this one makes the binding immediate
        if(toSyncAgainst == null) {
            widget.addWidgetProcessor(new ImmediateBinder());
        }
        else {
            //this one makes binding almost immediate: waits for the guischedule
            widget.addWidgetProcessor(new GUISyncedBinder(toSyncAgainst));
            //with GUI state we get the randomizer and so we can swap strategies on the fly
            widget.addWidgetProcessor(new StrategyWidgetProcessor(toSyncAgainst));
        }
        // this one creates the combo-boxes for strategy factories
        widget.addWidgetProcessor(new StrategyFactoryWidgetProcessor());
        //creates combo-boxes for double parameters
        widget.addWidgetProcessor(new DoubleParameterWidgetProcessor());
    }
}