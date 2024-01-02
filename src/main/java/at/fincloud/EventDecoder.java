package at.fincloud;

import org.reflections.Reflections;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventDecoder {

    private final String methodName;
    private final List<TypeReference<?>> parameters;

    private static final Map<String, Class<? extends Type>> typesMap;

    static {
        typesMap = new HashMap<>();
        Reflections reflections = new Reflections(Type.class.getPackage().getName());
        reflections.getSubTypesOf(Type.class).forEach(typeClass -> {
            if (!Modifier.isAbstract(typeClass.getModifiers())) {
                typesMap.put(typeClass.getSimpleName().toLowerCase(), typeClass);
            }
        });
        typesMap.put("string", Utf8String.class);
    }

    public EventDecoder(String name, List<String> typeNames) {
        this.methodName = name;
        this.parameters = typeNames.stream().map(
                typeName -> TypeReference.create(typesMap.get(typeName))
        ).collect(Collectors.toList());
    }

    public Event buildEvent() {
        return new Event(methodName, parameters);
    }
}
