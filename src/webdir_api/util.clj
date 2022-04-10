(ns webdir-api.util)

(defn flatten-entity
  [entity]
  (reduce-kv
    (fn [acc k v]
      (if (map? v)
        (merge
          acc
          (reduce-kv (fn [acc2 k2 v2]
                       (assoc acc2 (keyword (name k) (name k2)) v2))
                     {}
                     v))
        (assoc acc k v)))
    {}
    entity))

(defn flatten-map [path m]
  (if (map? m)
    (mapcat (fn [[k v]] (flatten-map (conj path k) v)) m)
    [[path m]]))

(defn find-in [coll x]
  (->> (flatten-map [] coll)
       (filter (fn [[_ v]] (= v x)))
       (map first)))