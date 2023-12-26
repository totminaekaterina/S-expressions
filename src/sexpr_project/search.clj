(ns sexpr-project.search
  (:require [clojure.string :as str])
  (:require [clojure.pprint :as pprint])
  (:require [sexpr-project.parser_1 :as parser]))

(def ^:dynamic *prev_query* "") ; переменная, где хранится предыдущий запрос

; в конце каждой ветви вызывается (search ((rest query) new_data)), где new_data - суженная область поиска данных в зависимости от прошлого результата

(defn- search
  [query data]

  (let [query_first (first query)
        cond-search (fn [query data]
                      (cond
                        (= (str (get query_first 0)) "*") (search (rest query) (vals data)) ; всё на подуровне
                        (str/includes? query_first "[") (let [q (parser/extract-conditions query_first) ; с условием
                                                              tag (first q)
                                                              condition (subs (second q) (inc (str/index-of (second q) "[")) (str/index-of (second q) "]"))]
                                                                          ;(println tag " " condition)
                                                                          ;(println (str/replace (subs condition 1) #"\"" ""))
                                                                          ;(println (get-in i [(keyword tag)]))
                                                          (cond
                                                            (= (str (get condition 0)) "=") (if (= (str (get-in data [(keyword tag)])) (str/replace (subs condition 1) #"\"" "")) ; если требуется равентсво данных в определённом поле
                                                                                              (search (rest query) data)
                                                                                              nil)
                                                            (= (str (get condition 0)) "%") (search (rest query) (remove nil? (doall (for [i (get-in data [(keyword tag)])] (remove empty? (doall (for [j (keys i)] (search [(str (subs (str j) 1) "[=" (subs condition 1) "]")] i)))))))) ;если требуется чтобы какое-то поле ниже было равно данным
                                                            (number? (Integer/parseInt condition)) (search (rest query) (get-in data [(keyword tag) (Integer/parseInt condition)])))) ; если индекс
                        :else (search (rest query) (get-in data [(keyword query_first)]))))]
    ;(println "Query: " query)
    ;(println (type query))
    ;(pprint/pprint data)
    (if (empty? query)
      (list data) ; если запрос закончился, то возвращаем найденные данные
      (cond
        (map? data) (cond-search query data)
        (vector? data) (doall (for [i data] (cond-search query i)))
        (seq? data) (doall (for [i data] (search query i)))
        :else nil))))

(defn start-search [query data]
  (if (not (= (str (get query 0)) "~")) ;если первый символ в строке ~, то относительный поиск
    (alter-var-root #'*prev_query* (constantly query))
    (alter-var-root #'*prev_query* (constantly (str *prev_query* (subs query 1))))) ;прикрепляем к прошлому поиску новую часть запроса
  ;(println *prev_query*)
  (remove nil? (flatten (search (parser/prepare-for-search *prev_query*) data))))

(defn- index-of-coll
  [item coll]
  (let [v (if
           (or (vector? coll) (string? coll))
            coll
            (apply vector coll))]
    (.indexOf coll item)))

(defn path-to-str [path]
  (let [res ""]
  (apply str (doall (for [i path] (if (number? i) 
                  (str res "[" i "]") 
                  (str res "/" (name i))))))))

(defn get-path [endpoint data]
  (let [search-path (fn [endpoint data] (cond (= endpoint data) []
                                              (map? data) (some (fn [[k v]]
                                                                  (when-let [p (get-path endpoint v)]
                                                                    (cons k p)))
                                                                data)
                                              (vector? data) (for [j data] (if (nil? (last (get-path endpoint j)))
                                                                             ()
                                                                             (list (index-of-coll j data) (get-path endpoint j))))
                                              :else nil))]
  ;(println endpoint)
  ;(println data)
  ;(println (type data))
  (if (seq? endpoint) (flatten (for [i endpoint] (search-path i data))) (search-path endpoint data))))