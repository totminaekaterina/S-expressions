<div align="center">
  <h1 align="center">
  Clojure S expressions
  </h1>
</div>

## About The Project

"S expressions" project as a part of NSU Modern Programming Methods course

## Contents

- [Object structure](#structure)
- [X-path like language to query elemets](#search)
- [Transform object into html](#transform)
- [Schema validation](#schema)

## Structure

We used regular Clojure edn data format, which is essentially a nested map with vectors

Examples of the syntax of the query language can are below:
```
* - match all
[] - optional selector 
   [0] - select by index, [="text"] - specific value equals "text", [%"text"] - one of the values equals "text"
catalog/* - return all books
catalog/genre[="Fantasy"] - return books with genre Fantasy
catalog[%"Fantasy"] - same, but without telling that it is genre, or if something else also equals "Fantasy" it should be returned
catalog[2] - return the third object on catalog
orders/*/name[%"Ellen Adams"] - return everything in "orders" that has a field name
~name или ~/name - относительный поиск
```

## Search

**start-search** function takes a query and data where the search will be conducted, as well as remembers the query in case next search will be relative
**get-path** function takes a value, path to which you want to find in your data, and data where search will be conducted, returns them as a list of keys and indexes
**path-to-str** function takes list and creates a string according to the query language

``` clojure
(start-search "/orders/addresses[%95819]/name/" (open-edn-from-string exml/ex_orders))
(search/get-path 99503 (open-edn-from-string exml/ex_orders))
(search/path-to-str (search/get-path (search/start-search "orders/addresses[%95819]/name" (open-edn-from-string exml/ex_orders)) (open-edn-from-string exml/ex_orders)))
```

## Modification

Modification has 3 functions: add-field, remove-field and modify-field.
**add-field** and **modify-field** take path to the value you want to add or modify and new value
**remove-field** only takes path to the value you want to remove

Exceptions:
**add-field** function throws an exception if field already exists
**modify-field** and **remove-field** throw an exception if field doesn't exists

``` clojure
(modify/add-field "/orders[0]/nothing/" (open-edn-from-string exml/ex_orders) "heh")
(modify/remove-field "/orders[0]/date/" (open-edn-from-string exml/ex_orders))
(modify/modify-field "/orders[0]/date/" (open-edn-from-string exml/ex_orders) "heh")
```

## Validation

Validation function **validate-by-schema** takes 2 edns: one with your data and the other that will serve as your schema
It then finds all paths it is possible to take in both documents and looks if your data has any paths that are not supposed to be in the document according to your schema

``` clojure
(def schema "{:orders [{:date nil :number nil :addresses [{:name nil :city nil :type nil :state nil :street nil :zip nil :country nil}] :items [{:ship_date nil :name nil :item nil :comment nil :quantity nil :price nil}]}]}")

(def ex_orders
  {:orders [{:number 99503
             :date "1999-10-20"

             :addresses
             [{:type "Shipping"
               :name "Ellen Adams"
               :street "123 Maple Street"
               :city "Mill Valley"
               :state "CA"
               :zip 10999
               :country "USA"}

              {:type "Billing"
               :name "Tai Yee"
               :street "8 Oak Avenue"
               :city "Old Town"
               :state "PA"
               :zip 95819
               :country "USA"}]

             :items
             [{:item "872-AA"
               :name "Lawnmower"
               :quantity 1
               :price 148.95
               :comment "comment"},

              {:item "926-AA"
               :name "Baby Monitor"
               :quantity 2
               :price 39.98
               :ship_date "1999-05-21"}]}]})

(validator/validate-by-schema schema (open-edn-from-string ex_orders))
```