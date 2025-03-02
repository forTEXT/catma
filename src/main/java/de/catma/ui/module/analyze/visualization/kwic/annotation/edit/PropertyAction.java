package de.catma.ui.module.analyze.visualization.kwic.annotation.edit;

public record PropertyAction(
		String propertyName, 
		PropertyActionType type,
		String value,
		String replaceValue) {
}
