//package com.agtinternational.iotcrawler.graphqlEnabler;
//
//import graphql.schema.DataFetcher;
//
//public class GraphQLInterfaces {
//    public interface Query {
//        public DataFetcher<IoTStream> streams(){  }
//        public DataFetcher<Iterable<Object>> sensors();
//        public DataFetcher<Iterable<Object>> systems();
//        public DataFetcher<Iterable<Object>> platforms();
//        public DataFetcher<Iterable<Object>> observableProperties();
//        public DataFetcher<Iterable<Object>> streamObservations();
//    }
//
//    public interface IoTStream {
//        public DataFetcher<String> id();
//        public DataFetcher<String> type();
//        public DataFetcher<Object> generatedBy();
//        public DataFetcher<Object> observes();
//        public DataFetcher<String> alternativeType();
//        public DataFetcher<String> dataProvider();
//    }
//
//    public interface System {
//        public DataFetcher<String> id();
//        public DataFetcher<String> type();
//        public DataFetcher<String> label();
//        public DataFetcher<Object> isHostedBy();
//    }
//
//    public interface Sensor {
//        public DataFetcher<Object> madeObservation();
//        public DataFetcher<Object> observes();
//        public DataFetcher<String> alternativeType();
//        public DataFetcher<String> location();
//    }
//
//    public interface Platform {
//        public DataFetcher<String> id();
//        public DataFetcher<String> type();
//        public DataFetcher<String> label();
//        public DataFetcher<Iterable<Object>> hosts();
//        public DataFetcher<String> location();
//        public DataFetcher<String> alternativeType();
//    }
//
//    public interface ObservableProperty {
//        public DataFetcher<String> id();
//        public DataFetcher<String> type();
//        public DataFetcher<String> label();
//        public DataFetcher<Iterable<Object>> isObservedBy();
//        public DataFetcher<String> alternativeType();
//    }
//
//    public interface StreamObservation {
//        public DataFetcher<String> id();
//        public DataFetcher<String> type();
//        public DataFetcher<Object> madeBySensor();
//        public DataFetcher<Object> belongsTo();
//        public DataFetcher<String> alternativeType();
//        public DataFetcher<String> hasSimpleResult();
//        public DataFetcher<String> resultTime();
//    }
//
//}
