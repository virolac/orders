import { useEffect, useState } from "react";
import { fetchOrders, type OrderResponse } from "./api";
import { useAuth } from "react-oidc-context";
import styles from "./MyOrders.module.css";
import { OrderRow } from "./OrderRow";

type MyOrdersLoadingState = {
  status: "loading";
};

type MyOrdersFailedState = {
  status: "error";
};

type MyOrdersSuccessState = {
  status: "success";
  orders: OrderResponse[];
};

type MyOrdersState =
  MyOrdersLoadingState | MyOrdersFailedState | MyOrdersSuccessState;

export function MyOrders() {
  const [myOrdersState, setMyOrdersState] = useState<MyOrdersState>({
    status: "loading",
  });
  const { user } = useAuth();

  useEffect(() => {
    const accessToken = user?.access_token;
    if (!accessToken) {
      return;
    }

    const controller = new AbortController();

    fetchOrders(accessToken, controller.signal)
      .then((orders) =>
        setMyOrdersState({
          status: "success",
          orders,
        }),
      )
      .catch((err) => {
        if (controller.signal.aborted) {
          return;
        }

        console.error(err);

        setMyOrdersState({
          status: "error",
        });
      });

    return () => controller.abort();
  }, [user]);

  if (myOrdersState.status === "loading") {
    return <p role="status">Loading your orders...</p>;
  } else if (myOrdersState.status === "error") {
    return (
      <p role="alert">Couldn't load your orders. Please try again later.</p>
    );
  } else if (myOrdersState.orders.length === 0) {
    return <p>You haven't placed any orders yet.</p>;
  }

  return (
    <ul className={styles.list}>
      {myOrdersState.orders.map((order) => (
        <OrderRow order={order} key={order.id} />
      ))}
    </ul>
  );
}
