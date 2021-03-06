package org.jpos.util;

import com.vaadin.data.Binder;
import com.vaadin.data.HasValue;
import com.vaadin.data.Validator;
import com.vaadin.data.converter.LocalDateToDateConverter;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.converter.StringToLongConverter;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.*;
import org.apache.commons.lang3.StringUtils;
import org.jpos.qi.QI;
import org.jpos.qi.ViewConfig;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.jpos.util.QIUtils.getCaptionFromId;

public class FieldFactory {
    private Object bean;
    private ViewConfig viewConfig;
    private Binder binder;

    public FieldFactory(Object bean, ViewConfig viewConfig, Binder binder) {
        this.bean = bean;
        this.viewConfig = viewConfig;
        this.binder = binder;
    }

    public AbstractField buildAndBindField (String id) throws NoSuchFieldException {
        Class dataType = getDataType(id);
        if (dataType.equals(Date.class)) {
            return buildAndBindTimestampField(id);
        } else if (dataType.equals(BigDecimal.class)) {
            return buildAndBindBigDecimalField(id);
        } else if (dataType.equals(Long.class) || dataType.equals(long.class)) {
            return buildAndBindLongField(id);
        } else if (dataType.equals(Integer.class) || dataType.equals(int.class)) {
            return buildAndBindIntField(id);
        } else if (dataType.equals(Short.class) || dataType.equals(short.class)) {
            return buildAndBindShortField(id);
        } else if (dataType.equals(Boolean.class) || dataType.equals(boolean.class)) {
            return buildAndBindBooleanField(id);
        } else if (dataType.equals(String.class)) {
            return buildAndBindTextField(id);
        } else {
            return new TextField("unconfigured data type " + dataType);
        }
    }

    private Class getDataType(String id) throws NoSuchFieldException {
        Object o = bean;
        try {
            return o.getClass().getDeclaredField(id).getType();
        } catch(NoSuchFieldException e) {
            return o.getClass().getSuperclass().getDeclaredField(id).getType();
        }
    }

    protected TextField buildAndBindLongField(String id) {
        TextField field = new TextField(getCaptionFromId("field." + id));
        Binder.BindingBuilder builder = formatField(id,field);
        builder = builder.withConverter(new StringToLongConverter(getApp().getMessage("errorMessage.NaN",id)));
        builder.bind(id);
        return field;
    }

    protected TextField buildAndBindIntField(String id) {
        TextField field = new TextField(getCaptionFromId("field." + id));
        Binder.BindingBuilder builder = formatField(id,field);
        builder = builder.withConverter(new StringToIntegerConverter(getApp().getMessage("errorMessage.NaN",id)));
        builder.bind(id);
        return field;
    }

    protected TextField buildAndBindShortField(String id) {
        TextField field = new TextField(getCaptionFromId("field." + id));
        Binder.BindingBuilder builder = formatField(id,field);
        builder = builder.withConverter(new StringToIntegerConverter(getApp().getMessage("errorMessage.NaN",id)));
        builder.bind(id);
        return field;
    }

    protected CheckBox buildAndBindBooleanField(String id) {
        CheckBox box = new CheckBox(StringUtils.capitalize(getCaptionFromId("field." + id)),false);
        Binder.BindingBuilder builder = formatField(id,box);
        builder.bind(id);
        return box;
    }

    protected TextField buildAndBindTextField(String id) {
        TextField field = new TextField(getCaptionFromId("field." + id));
        Binder.BindingBuilder builder = formatField(id,field);
        builder.bind(id);
        return field;
    }


    protected TextField buildAndBindTimestampField(String id) {
        TextField field = new TextField(getCaptionFromId("field." + id));
        getBinder().forField(field).withConverter(toModel -> null, toPresentation -> {
            if (toPresentation != null) {
                DateFormat dateFormat = new SimpleDateFormat(getApp().getMessage("timestampformat"));
                return dateFormat.format(toPresentation);
            }
            return "";
        }).bind(id);
        return field;
    }

    public DateField buildAndBindDateField(String id) {
        DateField dateField = new DateField(getCaptionFromId("field." + id));
        List<Validator> v = getValidators(id);
        Binder.BindingBuilder builder = getBinder().forField(dateField);
        for (Validator val : v)
            builder.withValidator(val);
        if (isRequired(id))
            builder.asRequired(getApp().getMessage("errorMessage.req", StringUtils.capitalize(getCaptionFromId("field."+id))));
        builder.withConverter(new LocalDateToDateConverter()).bind(id);
        return dateField;
    }

    protected TextField buildAndBindBigDecimalField(String id) {
        TextField field = new TextField(getCaptionFromId("field." + id));
        Binder.BindingBuilder builder = formatField(id,field);
        builder = builder.withConverter(new AmountConverter(getApp().getMessage("errorMessage.NaN",id)));
        builder.withNullRepresentation(BigDecimal.ZERO).bind(id);
        return field;
    }

    public Binder.BindingBuilder formatField(String id, HasValue field) {
        List<Validator> v = getValidators(id);
        Binder.BindingBuilder builder = getBinder().forField(field);
        for (Validator val : v)
            builder.withValidator(val);
        if (isRequired(id))
            builder.asRequired(getApp().getMessage("errorMessage.req",StringUtils.capitalize(getCaptionFromId("field."+id))));
        ViewConfig.FieldConfig config = viewConfig.getFields().get(id);
        String width = config != null ? config.getWidth() : null;
        if (field instanceof AbstractComponent)
            ((AbstractComponent)field).setWidth(width);
        builder = builder.withNullRepresentation("");
        return builder;
    }

    //Reads regex and length from 00_qi.xml
    //Override to add more customValidators
    public List<Validator> getValidators(String propertyId) {
        List<Validator> validators = new ArrayList<>();
        ViewConfig.FieldConfig config = viewConfig.getFields().get(propertyId);
        if (config != null) {
            String regex = config.getRegex();
            int length = config.getLength();
            String[] options = config.getOptions();
            if (options != null) {
                //Change the field to a Combo loaded with the options
                ComboBox combo = new ComboBox(getCaptionFromId("field."+propertyId),Arrays.asList(options));
                getBinder().bind(combo,propertyId);
                return null;
            }
            if (regex != null)
                validators.add(new RegexpValidator(getApp().getMessage("errorMessage.invalidField", propertyId),regex));
            if (length > 0)
                validators.add(new StringLengthValidator(getApp().getMessage("errorMessage.invalidField", propertyId),0,length));
        }
        return validators;
    }

    public boolean isRequired(String propertyId) {
        return viewConfig.getFields().get(propertyId).isRequired();
    }

    public Binder getBinder() {
        return binder;
    }

    public void setBinder(Binder binder) {
        this.binder = binder;
    }

    public QI getApp () {
        return (QI) QI.getCurrent();
    }
}
