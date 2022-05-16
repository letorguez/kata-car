import io.vavr.control.Either;

import java.util.*;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Configuration{
    private Set<Item> items = new HashSet<Item>();

    public Configuration() {}

    private Configuration(HashSet<Item> items){
        this.items = items;
    }

    public Set<Item> items(){
        return items;
    }

    public Configuration addItem(Item item) {
        var newItems = new HashSet<>(items);
        newItems.add(item);
        return new Configuration(newItems);
    }

    public Configuration addPackage(Package itemPackage) {
        var newItems = new HashSet<>(items);
        newItems.addAll(itemPackage.items());
        return new Configuration(newItems);
    }
}

class CatalogRule {
    public Item condition;
    public Item mandatoryAssociatedItem;

    public CatalogRule(Item condition, Item mandatoryAssociatedItem) {
        this.condition = condition;
        this.mandatoryAssociatedItem = mandatoryAssociatedItem;
    }

    public boolean matchesAnyItemIn(Set<Item> items) {
        return items.contains(condition);
    }
}

class CatalogRules{
    private Map<Item, List<Item>> rules = new HashMap<>();

    public void addRule(Item target, Item mandatoryDependency) {
        if (rules.containsKey(target)) {
            rules.get(target).add(mandatoryDependency);
        } else {
            rules.put(target, List.of(mandatoryDependency));
        }
    }
}

class Item{
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id.equals(item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private String id;

    public String id() {
        return id;
    }

    public Item(String id) {
        this.id = id;
    }
}

class Package{
    private List<Item> items;

    public Package(Item ...items) {
        this.items = List.of(items);
    }

    public List<Item> items() {
        return items;
    }
}

class CarConfigurationElement {
    private final Either<Item, Package> element;

    CarConfigurationElement(Either<Item, Package> element) {
        this.element = element;
    }

    CarConfigurationElement(Item item) {
        this.element = Either.left(item);
    }

    CarConfigurationElement(Package aPackage) {
        this.element = Either.right(aPackage);
    }

    public Item getItem(){
        return element.getLeft();
    }

    public Package getPackage(){
        return element.get();
    }

    public boolean isItem(){
        return element.isLeft();
    }

    public boolean isPackage(){
        return element.isRight();
    }
}

class CarConfiguratorShould {
    // TODO: explore the idea of a builder pattern
    public Configuration addToConfiguration(CatalogRules rules, CarConfigurationElement element,
                                            Configuration configuration) {
        if (element.isItem()) {
            configuration = configuration.addItem(element.getItem());
            int i = 0;
            while (i < rules.size()) {
                CatalogRule rule = rules.get(i);
                var items = configuration.items();
                i++;
                if (rule.matchesAnyItemIn(items) && !items.contains(rule.mandatoryAssociatedItem)){
                    configuration = configuration.addItem(rule.mandatoryAssociatedItem);
                    i = 0;
                }
            }
            return configuration;
        }
        return configuration.addPackage(element.getPackage());
    }


    // TODO list:
    //   añadir item a configuracion sin reglas
    //   añadir package a configuration sin reglas
    //   si X -> Y, y añado X, entonces configuration tiene X e Y
    //   si X -> Y y si Y -> Z, entonces configuration tiene X, Y, Z
    //   si X ==> Y o bien X ==> Z, entonces configuration esta incompleta porque necesita Y o Z
    //   Si X -> Y y Añado paquete {Z, X}, entonces configuration tiene Z, X e Y
    //   Si configuration tiene X y regla dice {X,Y} ==> Z y añadimos Y, entonces configuration tiene X, Y, Z
    //   Si configuration tiene X y regla dice que X -> !Y, entonces si intento añadir Y, la configuración es inválida porque X no puede ir con Y
    //   Si X -> Y y Y -> X, Si elijo X o elijo Y, entonces configuration tiene X e Y

    @Test
    public void bePossibleToAddAnItem(){
        Configuration config = new Configuration();

        config = addToConfiguration(new CatalogRules(),
                new CarConfigurationElement(new Item("X")), config);

        HashSet<Item> expected = new HashSet<>(Arrays.asList(new Item("X")));
        assertEquals(expected, config.items());
    }
    @Test
    public void bePossibleToAddAPackage(){
        Configuration config = new Configuration();

        config = addToConfiguration(new CatalogRules(),
                new CarConfigurationElement(
                        new Package(new Item("X"), new Item("Y"))), config);

        HashSet<Item> expected = new HashSet<>(Arrays.asList(new Item("X"), new Item("Y")));
        assertEquals(expected, config.items());
    }

    @Test
    public void addAnotherItemIfARuleDemandsIt(){
        Configuration config = new Configuration();
        CatalogRules rules = new CatalogRules();
        rules.add(new CatalogRule(new Item("X"), new Item("Y")));

        config = addToConfiguration(rules,
                new CarConfigurationElement(new Item("X")), config);

        HashSet<Item> expected = new HashSet<>(Arrays.asList(new Item("X"), new Item("Y")));
        assertEquals(expected, config.items());
    }

    @Test
    public void addMultipleItemsIfDifferentRulesDemandIt(){
        Configuration config = new Configuration();
        CatalogRules rules = new CatalogRules();
        rules.add(new CatalogRule(new Item("Y"), new Item("Z")));
        rules.add(new CatalogRule(new Item("X"), new Item("Y")));

        config = addToConfiguration(rules, new CarConfigurationElement(new Item("X")), config);

        HashSet<Item> expected = new HashSet<>(Arrays.asList(new Item("X"), new Item("Y"), new Item("Z")));
        assertEquals(expected, config.items());
    }
}
