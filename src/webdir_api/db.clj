(ns webdir-api.db
  (:require [clojure.data.json :as json]
            [schema.core :as s]))

(s/defschema Name
             {:first                  s/Str
              :last                   s/Str
              (s/optional-key :title) s/Str})

(s/defschema Picture
             {:large     s/Str
              :medium    s/Str
              :thumbnail s/Str})

(s/defschema Employee
             {:name                     Name
              :email                    s/Str
              :phone                    s/Str
              (s/optional-key :picture) Picture
              :department               s/Str})

(s/defschema EmployeeID
             {:id s/Int})

(s/defschema EmployeeWithId
             (assoc Employee (s/optional-key :id) s/Int))

(s/defschema EmployeesList
             {:employees [EmployeeWithId]})

;; seed our data with randomuser.me api
(def ^:private person-data-url "https://randomuser.me/api/?dataType=json&seed=jtth&results=100&inc=name,email,phone,picture&noinfo")
(defonce ^:private imported-employee-list (:results (json/read-str (slurp person-data-url)
                                                                   :key-fn keyword)))

;; add departments
(def ^:private departments ["HR" "Marketing" "Legal" "Engineering"])
(def ^:private departments-kw [:department/engineering
                               :department/hr
                               :department/legal
                               :department/marketing])
(defn- add-departments
  "adds department key with random value"
  [employee-list]
  (let [random-department (fn [] {:department (rand-nth departments)})
        merge-department (fn [employee] (merge employee (random-department)))]
    (map merge-department employee-list)))

(def ^:private employees-vec (add-departments imported-employee-list))

;; combine into a map of ids, then make our "database"
(def ^:private dbseed
  (let [id-numbers (range 1 (+ 1 (count employees-vec)))
        merge-id (fn [employee id] (merge employee {:id id}))]
    (zipmap id-numbers (map merge-id employees-vec id-numbers))))

;; our "database"
(defonce the-employees (atom dbseed))

(comment
  ;; does the raw data look okay?
  (keys (first imported-employee-list)))


