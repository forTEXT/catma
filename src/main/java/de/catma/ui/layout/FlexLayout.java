package de.catma.ui.layout;

import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;

/**
 * Created by jonte on 17/03/2017.
 */
public abstract class FlexLayout extends CssLayout {

    public static final String FLEXLAYOUT = "flexlayout";
    private AlignContent alignContent;
    private AlignItems alignItems;
    private AlignSelf alignSelf;
    private FlexDirection flexDirection;
    private FlexWrap flexWrap;
    private JustifyContent justifyContent;
    private Overflow overflow;
    private Position position;

    public FlexLayout(Component... components) {
        setPrimaryStyleName(FLEXLAYOUT);
        addComponents(components);
    }

    public FlexLayout(FlexDirection flexDirection, Component... components) {
        setPrimaryStyleName(FLEXLAYOUT);
        addComponents(components);
        setFlexDirection(flexDirection);
    }

    public AlignContent getAlignContent() {
        return alignContent;
    }

    public void setAlignContent(AlignContent alignContent) {
        this.alignContent = alignContent;
        for (AlignContent value : AlignContent.values()) {
            removeStyleName(value.getStyleName());
        }
        addStyleName(this.alignContent.getStyleName());
    }

    public AlignItems getAlignItems() {
        return alignItems;
    }

    public void setAlignItems(AlignItems alignItems) {
        this.alignItems = alignItems;
        for (AlignItems value : AlignItems.values()) {
            removeStyleName(value.getStyleName());
        }
        addStyleName(this.alignItems.getStyleName());
    }

    public AlignSelf getAlignSelf() {
        return alignSelf;
    }

    public void setAlignSelf(AlignSelf alignSelf) {
        this.alignSelf = alignSelf;
        for (AlignSelf value : AlignSelf.values()) {
            removeStyleName(value.getStyleName());
        }
        addStyleName(this.alignSelf.getStyleName());
    }

    public FlexDirection getFlexDirection() {
        return flexDirection;
    }

    public void setFlexDirection(FlexDirection flexDirection) {
        this.flexDirection = flexDirection;
        for (FlexDirection value : FlexDirection.values()) {
            removeStyleName(value.getStyleName());
        }
        addStyleName(this.flexDirection.getStyleName());
    }

    public FlexWrap getFlexWrap() {
        return flexWrap;
    }

    public void setFlexWrap(FlexWrap flexWrap) {
        this.flexWrap = flexWrap;
        for (FlexWrap value : FlexWrap.values()) {
            removeStyleName(value.getStyleName());
        }
        addStyleName(this.flexWrap.getStyleName());
    }

    public JustifyContent getJustifyContent() {
        return justifyContent;
    }

    public void setJustifyContent(JustifyContent justifyContent) {
        this.justifyContent = justifyContent;
        for (JustifyContent value : JustifyContent.values()) {
            removeStyleName(value.getStyleName());
        }
        addStyleName(this.justifyContent.getStyleName());
    }

    public Overflow getOverflow() {
        return overflow;
    }

    public void setOverflow(Overflow overflow) {
        this.overflow = overflow;
        for (Overflow value : Overflow.values()) {
            removeStyleName(value.getStyleName());
        }
        addStyleName(this.overflow.getStyleName());
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
        for (Position value : Position.values()) {
            removeStyleName(value.getStyleName());
        }
        addStyleName(this.position.getStyleName());
    }

    public enum AlignContent {
        CENTER("align-content-center"),
        FLEX_END("align-content-flex-end"),
        FLEX_START("align-content-flex-start"),
        SPACE_AROUND("align-content-space-around"),
        SPACE_BETWEEN("align-content-space-between"),
        STRETCH("align-content-stretch");

        private final String styleName;

        AlignContent(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }

    public enum AlignItems {
        BASELINE("align-items-baseline"),
        CENTER("align-items-center"),
        FLEX_END("align-items-flex-end"),
        FLEX_START("align-items-flex-start"),
        STRETCH("align-items-stretch");

        private final String styleName;

        AlignItems(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }

    public enum AlignSelf {
        BASELINE("align-self-baseline"),
        CENTER("align-self-center"),
        FLEX_END("align-self-flex-end"),
        FLEX_START("align-self-flex-start"),
        STRETCH("align-self-stretch");

        private final String styleName;

        AlignSelf(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }

    public enum FlexDirection {
        ROW("row"),
        ROW_REVERSE("row-reverse"),
        COLUMN("column"),
        COLUMN_REVERSE("column-reverse");

        private final String styleName;

        FlexDirection(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }

    public enum FlexWrap {
        NOWRAP("flex-nowrap"),
        WRAP("flex-wrap"),
        WRAP_REVERSE("flex-wrap-reverse");

        private final String styleName;

        FlexWrap(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }

    public enum JustifyContent {
        CENTER("justify-content-center"),
        FLEX_END("justify-content-flex-end"),
        FLEX_START("justify-content-flex-start"),
        SPACE_AROUND("justify-content-space-around"),
        SPACE_BETWEEN("justify-content-space-between");

        private final String styleName;

        JustifyContent(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }

    public enum Overflow {
        AUTO("overflow-auto"),
        HIDDEN("overflow-hidden"),
        SCROLL("overflow-scroll"),
        VISIBLE("overflow-visible");

        private final String styleName;

        Overflow(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }

    public enum Position {
        ABSOLUTE("position-absolute"),
        FIXED("position-fixed"),
        RELATIVE("position-relative"),
        STATIC("position-static"),
        STICKY("position-sticky");

        private final String styleName;

        Position(String styleName) {
            this.styleName = styleName;
        }

        public String getStyleName() {
            return styleName;
        }
    }

}
