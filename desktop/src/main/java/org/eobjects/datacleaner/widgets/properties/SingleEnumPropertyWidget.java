/**
 * DataCleaner (community edition)
 * Copyright (C) 2013 Human Inference
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.datacleaner.widgets.properties;

import javax.inject.Inject;

import org.eobjects.analyzer.descriptors.ConfiguredPropertyDescriptor;
import org.eobjects.analyzer.job.builder.AbstractBeanJobBuilder;
import org.eobjects.datacleaner.widgets.DCComboBox;
import org.eobjects.datacleaner.widgets.DCComboBox.Listener;
import org.eobjects.datacleaner.widgets.EnumComboBoxListRenderer;
import org.apache.metamodel.util.CollectionUtils;

public final class SingleEnumPropertyWidget extends AbstractPropertyWidget<Enum<?>> {

	private final DCComboBox<Enum<?>> _comboBox;

	@Inject
	public SingleEnumPropertyWidget(ConfiguredPropertyDescriptor propertyDescriptor,
			AbstractBeanJobBuilder<?, ?, ?> beanJobBuilder) {
		super(beanJobBuilder, propertyDescriptor);

		Enum<?>[] enumConstants = (Enum<?>[]) propertyDescriptor.getType().getEnumConstants();

		if (!propertyDescriptor.isRequired()) {
			enumConstants = CollectionUtils.array(new Enum<?>[] { null }, enumConstants);
		}

		_comboBox = new DCComboBox<Enum<?>>(enumConstants);
		_comboBox.setRenderer(new EnumComboBoxListRenderer());

		Enum<?> currentValue = getCurrentValue();
		_comboBox.setSelectedItem(currentValue);

		addComboListener(new Listener<Enum<?>>() {
			@Override
			public void onItemSelected(Enum<?> item) {
				fireValueChanged();
			}
		});
		add(_comboBox);
	}
	
	public void addComboListener(Listener<Enum<?>> listener) {
		_comboBox.addListener(listener);
	}

	@Override
	public Enum<?> getValue() {
		return (Enum<?>) _comboBox.getSelectedItem();
	}

	@Override
	protected void setValue(Enum<?> value) {
		_comboBox.setSelectedItem(value);
	}

}
