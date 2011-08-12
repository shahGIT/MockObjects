package com.develogical.shopping;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.equalTo;

@RunWith(JMock.class)
public class TestOrderDispatcher {

    static final Order ORDER = new Order("Continuous Delivery");
    static final Order ORDER2 = new Order("Growing Object-Oriented Software");

    static final Item BOOK = new Book("Continuous Delivery");
    static final Item BOOK2 = new Book("Growing Object-Oriented Software");

    Mockery context = new Mockery();

    Warehouse warehouse = context.mock(Warehouse.class);
    OrderTracker orderTracker = context.mock(OrderTracker.class);
    DeliveryVehicle truck = context.mock(DeliveryVehicle.class, "Truck 1");

    Recipient recipient = new Recipient("Fred Bloggs", "1 Park Road, Oxford", "fred@example.com");
    Recipient recipient2 = new Recipient("John Smith", "99 Acacia Avenue, London", "john@example.com");

    OrderDispatcher dispatcher = new OrderDispatcher(warehouse);

    @Before
    public void stockWarehouse() {
         context.checking(new Expectations() {{
            allowing(warehouse).retrieve(ORDER); will(returnValue(BOOK));
            allowing(warehouse).retrieve(ORDER2); will(returnValue(BOOK2));
        }});
    }

    @Test
    public void ifItemIsInStockThenOrderIsConfirmed() {

        context.checking(new Expectations() {{
            ignoring(truck);
            allowing(warehouse).hasStockOf(ORDER); will(returnValue(true));
            one(orderTracker).orderConfirmed(ORDER);
        }});

        dispatcher.placeOrder(recipient, ORDER, orderTracker);
    }

    @Test
    public void ifItemIsNotInStockThenOrderShouldBeMarkedOutOfStock() {

        context.checking(new Expectations() {{
            ignoring(truck);
            allowing(warehouse).hasStockOf(ORDER); will(returnValue(false));
            one(orderTracker).outOfStock(ORDER);
        }});

        dispatcher.placeOrder(recipient, ORDER, orderTracker);
    }

    @Test
    public void packetsAreNotSentOutForOutOfStockItems() {

        context.checking(new Expectations() {{
            ignoring(orderTracker);
            allowing(warehouse).hasStockOf(ORDER); will(returnValue(false));
            never(truck).deliver(with(equalTo(recipient)), with(any(Parcel.class)));
        }});

        dispatcher.placeOrder(recipient, ORDER, orderTracker);
    }

    @Test
    public void twoOrdersPlacedByDifferentRecipientsAreSentAsTwoSeparatePackets() {

        context.checking(new Expectations() {{
            allowing(warehouse).hasStockOf(with(any(Order.class))); will(returnValue(true));
            ignoring(orderTracker);
            one(truck).deliver(recipient, parcelContaining(BOOK));
            one(truck).deliver(recipient2, parcelContaining(BOOK2));
        }});

        dispatcher.placeOrder(recipient, ORDER, orderTracker);
        dispatcher.placeOrder(recipient2, ORDER2, orderTracker);

        dispatcher.vehicleArrived(truck);
        dispatcher.vehicleDeparted(truck);
    }

    @Test
    public void twoOrdersByTheSamePersonPlacedBeforeTheVehicleArrivesAreCombinedIntoOnePacket() {

        context.checking(new Expectations() {{
            allowing(warehouse).hasStockOf(with(any(Order.class))); will(returnValue(true));
            ignoring(orderTracker);
            one(truck).deliver(recipient, parcelContaining(BOOK, BOOK2));
        }});

        dispatcher.placeOrder(recipient, ORDER, orderTracker);
        dispatcher.placeOrder(recipient, ORDER2, orderTracker);

        dispatcher.vehicleArrived(truck);
        dispatcher.vehicleDeparted(truck);
    }

    @Test
    public void ifTheTruckArrivesBeforeYourSecondOrderIsPlacedYouWillReceiveTwoSeparatePackets() {

        context.checking(new Expectations() {{
            allowing(warehouse).hasStockOf(with(any(Order.class))); will(returnValue(true));
            ignoring(orderTracker);
            one(truck).deliver(recipient, parcelContaining(BOOK));
            one(truck).deliver(recipient, parcelContaining(BOOK2));
        }});

        dispatcher.placeOrder(recipient, ORDER, orderTracker);
        dispatcher.vehicleArrived(truck);
        dispatcher.placeOrder(recipient, ORDER2, orderTracker);
        dispatcher.vehicleDeparted(truck);
    }

    @Test
    public void ifTheTruckDepartsBeforeYourSecondOrderIsPlacedYouWillReceiveTwoSeparateDeliveries() {

        context.checking(new Expectations() {{
            allowing(warehouse).hasStockOf(with(any(Order.class))); will(returnValue(true));
            ignoring(orderTracker);
            one(truck).deliver(recipient, parcelContaining(BOOK));
            one(truck).deliver(recipient, parcelContaining(BOOK2));
        }});

        dispatcher.placeOrder(recipient, ORDER, orderTracker);
        dispatcher.vehicleArrived(truck);
        dispatcher.vehicleDeparted(truck);

        dispatcher.placeOrder(recipient, ORDER2, orderTracker);
        dispatcher.vehicleArrived(truck);
        dispatcher.vehicleDeparted(truck);
    }

    private Parcel parcelContaining(Item... items) {
        return new Parcel(items);
    }
}
