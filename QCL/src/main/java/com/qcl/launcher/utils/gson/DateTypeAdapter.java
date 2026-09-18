package com.qcl.launcher.utils.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

/* loaded from: classes2.dex */
public final class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    public static final DateTypeAdapter INSTANCE = new DateTypeAdapter();
    public static final DateFormat EN_US_FORMAT = DateFormat.getDateTimeInstance(2, 2, Locale.US);
    public static final DateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    public static final DateTimeFormatter ISO_DATE_TIME = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd().optionalStart().appendOffset("+HHMM", "+0000").optionalEnd().optionalStart().appendOffset("+HH", "Z").optionalEnd().optionalStart().appendOffsetId().optionalEnd().toFormatter();

    private DateTypeAdapter() {
    }

    @Override // com.google.gson.JsonSerializer
    public JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonPrimitive jsonPrimitive;
        synchronized (EN_US_FORMAT) {
            jsonPrimitive = new JsonPrimitive(serializeToString(date));
        }
        return jsonPrimitive;
    }

    @Override // com.google.gson.JsonDeserializer
    public Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (!(jsonElement instanceof JsonPrimitive)) {
            throw new JsonParseException("The date should be a string value");
        }
        Date deserializeToDate = deserializeToDate(jsonElement.getAsString());
        if (type == Date.class) {
            return deserializeToDate;
        }
        throw new IllegalArgumentException(getClass().toString() + " cannot be deserialized to " + type);
    }

    /* JADX WARN: Type inference failed for: r2v3, types: [java.time.ZonedDateTime] */
    public static Date deserializeToDate(String str) {
        Date parse;
        DateFormat dateFormat = EN_US_FORMAT;
        synchronized (dateFormat) {
            try {
                try {
                    parse = dateFormat.parse(str);
                } catch (DateTimeParseException e) {
                    try {
                        return Date.from(LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(ZoneId.systemDefault()).toInstant());
                    } catch (DateTimeParseException unused) {
                        throw new JsonParseException("Invalid date: " + str, e);
                    }
                }
            } catch (ParseException unused2) {
                return Date.from(ZonedDateTime.parse(str, ISO_DATE_TIME).toInstant());
            }
        }
        return parse;
    }

    public static String serializeToString(Date date) {
        String str;
        synchronized (EN_US_FORMAT) {
            String format = ISO_8601_FORMAT.format(date);
            str = format.substring(0, 22) + ":" + format.substring(22);
        }
        return str;
    }
}
