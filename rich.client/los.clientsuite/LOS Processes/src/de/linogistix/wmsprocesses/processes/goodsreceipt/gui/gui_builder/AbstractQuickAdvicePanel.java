/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * QuickAdvicePanel.java
 *
 * Created on 16.02.2014, 22:37:50
 */

package de.linogistix.wmsprocesses.processes.goodsreceipt.gui.gui_builder;

import de.linogistix.wmsprocesses.res.WMSProcessesBundleResolver;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 *
 * @author Trautmann
 */
public class AbstractQuickAdvicePanel extends javax.swing.JPanel {

    /** Creates new form QuickAdvicePanel */
    public AbstractQuickAdvicePanel() {
        initComponents();
        // Use tab to move focus
        descriptionArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.getModifiers() > 0) {
                        descriptionArea.transferFocusBackward();
                    } else {
                        descriptionArea.transferFocus();
                    }
                    e.consume();
                }
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        clientCombo = new de.linogistix.common.gui.component.controls.BOAutoFilteringComboBox();
        itemDataCombo = new de.linogistix.common.gui.component.controls.BOAutoFilteringComboBox();
        amountTextField = new de.linogistix.common.gui.component.controls.LOSNumericFormattedTextField();
        amountLabel = new de.linogistix.common.gui.component.controls.LosLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionArea = new javax.swing.JTextArea();

        setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(clientCombo, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 0);
        add(itemDataCombo, gridBagConstraints);

        amountTextField.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 0);
        add(amountTextField, gridBagConstraints);

        amountLabel.setText(org.openide.util.NbBundle.getMessage(WMSProcessesBundleResolver.class, "Amount")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 0);
        add(amountLabel, gridBagConstraints);

        descriptionArea.setColumns(20);
        descriptionArea.setRows(5);
        jScrollPane1.setViewportView(descriptionArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(5, 20, 0, 0);
        add(jScrollPane1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private de.linogistix.common.gui.component.controls.LosLabel amountLabel;
    protected de.linogistix.common.gui.component.controls.LOSNumericFormattedTextField amountTextField;
    protected de.linogistix.common.gui.component.controls.BOAutoFilteringComboBox clientCombo;
    protected javax.swing.JTextArea descriptionArea;
    protected de.linogistix.common.gui.component.controls.BOAutoFilteringComboBox itemDataCombo;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

}