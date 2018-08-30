/*
 * AppearanceOptionPane.java - Appearance options panel
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2004 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.options;

//{{{ Imports
import javax.swing.*;

import java.awt.Font;
import java.awt.event.*;
import java.io.*;

import org.gjt.sp.jedit.gui.FontSelector;
import org.gjt.sp.jedit.gui.NumericTextField;

import org.gjt.sp.jedit.*;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.IOUtilities;
//}}}

public class AppearanceOptionPane extends AbstractOptionPane implements ItemListener
{
	/**
	 * List of icon themes that are supported in jEdit core.
	 * Possible values of the jedit property 'icon-theme'
	 */
	public static final String[] builtInIconThemes = {"tango", "old"};

	//{{{ AppearanceOptionPane constructor
	public AppearanceOptionPane()
	{
		super("appearance");
	} //}}}

	//{{{ _init() method
	@Override
	protected void _init()
	{
		/* Look and feel */
//		addComponent(new JLabel(jEdit.getProperty("options.appearance.lf.note")));

		lfs = UIManager.getInstalledLookAndFeels();
		String[] names = new String[lfs.length];
		String lf = UIManager.getLookAndFeel().getClass().getName();
		int index = 0;
		for(int i = 0; i < names.length; i++)
		{
			names[i] = lfs[i].getName();
			if(lf.equals(lfs[i].getClassName()))
				index = i;
		}

		lookAndFeel = new JComboBox<String>(names);
		lookAndFeel.setSelectedIndex(index);

		addComponent(jEdit.getProperty("options.appearance.lf"),
			lookAndFeel);


		/* Icon Theme */
		String[] themes = IconTheme.builtInNames();
		iconThemes = new JComboBox<String>(themes);
		addComponent(jEdit.getProperty("options.appearance.iconTheme"), iconThemes);
		String oldTheme = IconTheme.get();
		for (int i=0; i<themes.length; ++i)
		{
			if (themes[i].equals(oldTheme))
			{
				iconThemes.setSelectedIndex(i);
				break;
			}
		}

		/* Primary Swing font for button, menu, and label components */
		Font pf = jEdit.getFontProperty("metal.primary.font");
		primaryFont = new FontSelector(pf);
		primaryFont.setEnabled(true);
		addComponent(jEdit.getProperty("options.appearance.primaryFont"),
			primaryFont);

		/* Secondary Swing font for list and textfield components */
		secondaryFont = new FontSelector(jEdit.getFontProperty(
			"metal.secondary.font"));
		secondaryFont.setEnabled(true);
		addComponent(jEdit.getProperty("options.appearance.secondaryFont"),
			secondaryFont);

		/* HelpViewer font */
		helpViewerFont = new FontSelector(jEdit.getFontProperty(
			"helpviewer.font", pf));
		addComponent(jEdit.getProperty("options.appearance.helpViewerFont"),
			helpViewerFont);

		/* History count */
		history = new NumericTextField(jEdit.getProperty("history"), true);
		addComponent(jEdit.getProperty("options.appearance.history"),history);

		/* Menu spillover count */
		menuSpillover = new NumericTextField(jEdit.getProperty("menu.spillover"), true);
		addComponent(jEdit.getProperty("options.appearance.menuSpillover"),menuSpillover);

		systemTrayIcon = new JCheckBox(jEdit.getProperty(
					"options.general.systrayicon", "Show the systray icon"));
		systemTrayIcon.setSelected(jEdit.getBooleanProperty("systrayicon", true));
		addComponent(systemTrayIcon);

		addSeparator("options.appearance.startup.label");

		/* Show splash screen */
		showSplash = new JCheckBox(jEdit.getProperty(
			"options.appearance.showSplash"));
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory == null)
			showSplash.setSelected(true);
		else
			showSplash.setSelected(!new File(settingsDirectory,"nosplash").exists());
		addComponent(showSplash);

		/* Show tip of the day */
		showTips = new JCheckBox(jEdit.getProperty(
			"options.appearance.showTips"));
		showTips.setSelected(jEdit.getBooleanProperty("tip.show"));
		addComponent(showTips);

		addSeparator("options.appearance.experimental.label");
		addComponent(GUIUtilities.createMultilineLabel(
			jEdit.getProperty("options.appearance.experimental.caption")));

		/* Use jEdit colors in all text components */
		textColors = new JCheckBox(jEdit.getProperty(
			"options.appearance.textColors"));
		textColors.setSelected(jEdit.getBooleanProperty("textColors"));
		addComponent(textColors);

		/* Decorate frames with look and feel (JDK 1.4 only) */
		decorateFrames = new JCheckBox(jEdit.getProperty(
			"options.appearance.decorateFrames"));
		decorateFrames.setSelected(jEdit.getBooleanProperty("decorate.frames"));
		addComponent(decorateFrames);

		/* Decorate dialogs with look and feel (JDK 1.4 only) */
		decorateDialogs = new JCheckBox(jEdit.getProperty(
			"options.appearance.decorateDialogs"));
		decorateDialogs.setSelected(jEdit.getBooleanProperty("decorate.dialogs"));
		addComponent(decorateDialogs);
		
		lnfChanged = false;
	} //}}}

	//{{{ _save() method
	@Override
	protected void _save()
	{
		if (lnfChanged)
		{
			String lf = lfs[lookAndFeel.getSelectedIndex()].getClassName();
			jEdit.setProperty("lookAndFeel", lf);
		}
		jEdit.setFontProperty("metal.primary.font",primaryFont.getFont());
		jEdit.setFontProperty("metal.secondary.font",secondaryFont.getFont());
		jEdit.setFontProperty("helpviewer.font", helpViewerFont.getFont());
		jEdit.setProperty("history",history.getText());
		jEdit.setProperty("menu.spillover",menuSpillover.getText());
		jEdit.setBooleanProperty("tip.show",showTips.isSelected());
		jEdit.setBooleanProperty("systrayicon", systemTrayIcon.isSelected());
		IconTheme.set(iconThemes.getSelectedItem().toString());

		// adjust swing properties for button, menu, and label, and list and 
		// textfield fonts
		setFonts();
		
		// This is handled a little differently from other jEdit settings
		// as this flag needs to be known very early in the
		// startup sequence, before the user properties have been loaded
		setFileFlag("nosplash", !showSplash.isSelected());

		jEdit.setBooleanProperty("textColors",textColors.isSelected());
		jEdit.setBooleanProperty("decorate.frames",decorateFrames.isSelected());
		jEdit.setBooleanProperty("decorate.dialogs",decorateDialogs.isSelected());
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private UIManager.LookAndFeelInfo[] lfs;
	private JComboBox<String> lookAndFeel;
	private FontSelector primaryFont;
	private FontSelector secondaryFont;
	private FontSelector helpViewerFont;

	private JTextField history;
	private JTextField menuSpillover;
	private JCheckBox showTips;
	private JCheckBox showSplash;
	private JCheckBox textColors;
	private JCheckBox decorateFrames;
	private JCheckBox decorateDialogs;
	private JComboBox<String> iconThemes;
	private JCheckBox systemTrayIcon;
	private boolean lnfChanged = false;
	//}}}

	//{{{ setFileFlag() method
	private void setFileFlag(String fileName, boolean present)
	{
		String settingsDirectory = jEdit.getSettingsDirectory();
		if(settingsDirectory != null)
		{
			File file = new File(settingsDirectory, fileName);
			if (!present)
			{
				file.delete();
			}
			else
			{
				FileOutputStream out = null;
				try
				{
					out = new FileOutputStream(file);
					out.write('\n');
					out.close();
				}
				catch(IOException io)
				{
					Log.log(Log.ERROR,this,io);
				}
				finally
				{
					IOUtilities.closeQuietly(out);
				}
			}
		}
	} //}}}
	
	//{{{ setFonts() method
	private void setFonts() {
		// "primary" font, for buttons, labels, menus, etc, components that just 
		// display text
		UIManager.put("Button.font", primaryFont.getFont());
		UIManager.put("CheckBox.font", primaryFont.getFont());
		UIManager.put("CheckBoxMenuItem.font", primaryFont.getFont());
		UIManager.put("ColorChooser.font", primaryFont.getFont());
		UIManager.put("DesktopIcon.font", primaryFont.getFont());
		UIManager.put("Label.font", primaryFont.getFont());
		UIManager.put("Menu.font", primaryFont.getFont());
		UIManager.put("MenuBar.font", primaryFont.getFont());
		UIManager.put("MenuItem.font", primaryFont.getFont());
		UIManager.put("OptionPane.font", primaryFont.getFont());
		UIManager.put("Panel.font", primaryFont.getFont());
		UIManager.put("PopupMenu.font", primaryFont.getFont());
		UIManager.put("ProgressBar.font", primaryFont.getFont());
		UIManager.put("RadioButton.font", primaryFont.getFont());
		UIManager.put("RadioButtonMenuItem.font", primaryFont.getFont());
		UIManager.put("ScrollPane.font", primaryFont.getFont());
		UIManager.put("Slider.font", primaryFont.getFont());
		UIManager.put("TabbedPane.font", primaryFont.getFont());
		UIManager.put("Table.font", primaryFont.getFont());
		UIManager.put("TableHeader.font", primaryFont.getFont());
		UIManager.put("TitledBorder.font", primaryFont.getFont());
		UIManager.put("ToggleButton.font", primaryFont.getFont());
		UIManager.put("ToolBar.font", primaryFont.getFont());
		UIManager.put("ToolTip.font", primaryFont.getFont());
		UIManager.put("Tree.font", primaryFont.getFont());
		UIManager.put("Viewport.font", primaryFont.getFont());
		
		// "secondary" font, for components the user can type into
		UIManager.put("ComboBox.font", secondaryFont.getFont());
		UIManager.put("EditorPane.font", secondaryFont.getFont());
		UIManager.put("FormattedTextField.font", secondaryFont.getFont());
		UIManager.put("List.font", secondaryFont.getFont());
		UIManager.put("PasswordField.font", secondaryFont.getFont());
		UIManager.put("Spinner.font", secondaryFont.getFont());
		UIManager.put("TextArea.font", secondaryFont.getFont());
		UIManager.put("TextField.font", secondaryFont.getFont());
		UIManager.put("TextPane.font", secondaryFont.getFont());		
	} //}}}
	
	
	
    //}}}
	
	// {{{ itemStateChanged() methos
    public final void itemStateChanged( ItemEvent evt ) 
    {
		lnfChanged = true;
    } // }}}
    
	    


}
