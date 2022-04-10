(ns webdir-api.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.interceptor.chain :refer [terminate]]
            [io.pedestal.interceptor :refer [interceptor]]
            [pedestal-api.core :as api]
            [pedestal-api.helpers :refer [before defbefore defhandler handler]]
            [schema.core :as s]
            [webdir-api.db :as db :refer [the-employees Name Picture Employee EmployeeID EmployeeWithId EmployeesList]]))


(def all-employees
  (api/annotate
    {:summary    "Get all employees in the list"
     :parameters {:query-params {(s/optional-key :sort) (s/enum :asc :desc)}}
     :responses  {200 {:body EmployeesList}}}
    (interceptor
      {:name  ::all-employees
       :enter (fn [ctx]
                (assoc ctx :response
                           {:status 200
                            :body   {:employees (let [sort (get-in ctx [:request :query-params :sort])]
                                                  (cond->> (vals @the-employees)
                                                           sort (sort-by :name)
                                                           (= :desc sort) reverse))}}))})))

(def create-employee
  (handler
    ::create-employee
    {:summary    "Create an employee"
     :parameters {:body-params Employee}
     :responses  {201 {:body EmployeeID}}}
    (fn [request]
      (let [id (inc (count @the-employees))
            employee-data (assoc (:body-params request) :id id)]
        (swap! the-employees assoc id employee-data)
        {:status 201
         :body   {:id id}}))))

(defbefore load-employee
           {:summary    "Load an employee by id"
            :parameters {:path-params {:id s/Int}}
            :responses  {404 {:body s/Str}}}
           [{:keys [request] :as context}]
           (if-let [employee (get @the-employees (get-in request [:path-params :id]))]
             (update context :request assoc :employee employee)
             (-> context terminate (assoc :response {:status 404
                                                     :body   "No employee found with this id"}))))

(defhandler get-employee
            {:summary    "Get an employee by id"
             :parameters {:path-params {:id s/Int}}
             :responses  {200 {:body EmployeeWithId}
                          404 {:body s/Str}}}
            [{:keys [employee] :as request}]
            {:status 200
             :body   employee})

(def update-employee
  (before
    ::update-employee
    {:summary    "Update an employee"
     :parameters {:path-params {:id s/Int}
                  :body-params Employee}
     :responses  {200 {:body s/Str}}}
    (fn [{:keys [request]}]
      (let [employee-id (get-in request [:path-params :id])]
        (swap! the-employees update employee-id merge (:body-params request))
        {:status 200
         :body   (str "employee " employee-id " updated")}))))

(def delete-employee
  (api/annotate
    {:summary    "Delete an employee by id"
     :parameters {:path-params {:id s/Int}}
     :responses  {200 {:body s/Str}}}
    (interceptor
      {:name  ::delete-employee
       :enter (fn [ctx]
                (let [employee (get-in ctx [:request :employee])]
                  (swap! the-employees dissoc (:id employee))
                  (assoc ctx :response
                             {:status 200
                              :body   (str "Deleted " (:name employee))})))})))

(def no-csp
  {:name  ::no-csp
   :leave (fn [ctx]
            (assoc-in ctx [:response :headers "Content-Security-Policy"] ""))})

(s/with-fn-validation
  (api/defroutes routes
                 {:info {:title       "Postlight employee directory api using pedestal-api"
                         :description "https://github.com/oliyh/pedestal-api"
                         :version     "1.0"}
                  :tags [{:name         "employees"
                          :description  "Everything about your employees"
                          :externalDocs {:description "jtth"
                                         :url         "https://jtth.net"}}
                         {:name        "employees"
                          :description "Operations about employees"}]}
                 [[["/api" ^:interceptors [api/error-responses
                                           (api/negotiate-response)
                                           (api/body-params)
                                           api/common-body
                                           (api/coerce-request)
                                           (api/validate-response)]
                    ["/employees" ^:interceptors [(api/doc {:tags ["employees"]})]
                     ["/" {:get  all-employees
                           :post create-employee}]
                     ["/:id" ^:interceptors [load-employee]
                      {:get    get-employee
                       :put    update-employee
                       :delete delete-employee}]]

                    ["/swagger.json" {:get api/swagger-json}]
                    ["/*resource" ^:interceptors [no-csp] {:get api/swagger-ui}]]]]))

(def service
  {:env                        :dev
   ::bootstrap/routes          #(deref #'routes)
   ;; linear-search, and declaring the swagger-ui handler last in the routes,
   ;; is important to avoid the splat param for the UI matching API routes
   ::bootstrap/router          :linear-search
   ::bootstrap/resource-path   "/public"
   ::bootstrap/type            :jetty
   ::bootstrap/allowed-origins {:allowed-origins (constantly true)
                                :creds           true}
   ::bootstrap/port            8080
   ::bootstrap/host            "0.0.0.0"                    ;; bind to all interfaces
   ::bootstrap/join?           false})