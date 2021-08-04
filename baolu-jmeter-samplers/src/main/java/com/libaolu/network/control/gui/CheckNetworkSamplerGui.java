package com.libaolu.network.control.gui;

import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;

import javax.swing.*;
import java.awt.*;

/**
 * @author libaolu
 * @date 2021/8/4 15:45
 */
public class CheckNetworkSamplerGui extends AbstractSamplerGui {

    /**
     * serverName or IP
     */
    private JTextField host;
    /**
     * port
     */
    private JTextField port;

    public CheckNetworkSamplerGui() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), "North");
        VerticalPanel mainPanel = new VerticalPanel();
        HorizontalPanel optionsPanel = new HorizontalPanel();
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
        optionsPanel.add(createHostOption());
        optionsPanel.add(createPortOption());
    }

    private JPanel createHostOption() {
        JLabel label = new JLabel("ServerName or IP");
        host = new JTextField(14);
        host.setMaximumSize(new Dimension(host.getPreferredSize()));
        label.setLabelFor(host);
        JPanel hostPanel = new JPanel(new FlowLayout());
        hostPanel.add(label);
        hostPanel.add(host);
        return hostPanel;
    }

    private JPanel createPortOption() {
        JLabel label = new JLabel("Port Number");
        port = new JTextField(10);
        port.setMaximumSize(new Dimension(port.getPreferredSize()));
        label.setLabelFor(port);
        JPanel portPanel = new JPanel(new FlowLayout());
        portPanel.add(label);
        portPanel.add(port);
        return portPanel;
    }

    @Override
    public String getLabelResource() {
        return null;
    }

    @Override
    public TestElement createTestElement() {
        return null;
    }

    @Override
    public void modifyTestElement(TestElement testElement) {

    }
}
