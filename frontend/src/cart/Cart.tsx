import { useState } from "react";
import { formatPrice } from "../format";
import { placeOrder, type OrderResponse } from "../orders/api";
import type { CartItem } from "./types";
import { OrderConfirmation } from "../orders/OrderConfirmation";
import { useAuth } from "react-oidc-context";
import styles from "./Cart.module.css";

function calculateEstimatedTotal(items: CartItem[]): number {
  return items.reduce((sum, { product, quantity }) => {
    return sum + product.price * quantity;
  }, 0);
}

type IdleState = {
  status: "idle";
};

type SubmittingState = {
  status: "submitting";
};

type FailedState = {
  status: "failed";
};

type PlacedState = {
  status: "placed";
  order: OrderResponse;
};

type OrderSubmitState = IdleState | SubmittingState | FailedState | PlacedState;

export type CartProps = {
  items: CartItem[];
  onOrderPlaced: () => void;
};

export function Cart({ items, onOrderPlaced }: CartProps) {
  const [orderState, setOrderState] = useState<OrderSubmitState>({
    status: "idle",
  });
  const { isLoading, isAuthenticated, signinRedirect, user } = useAuth();

  const handlePlaceOrder = async () => {
    const accessToken = user?.access_token;
    if (!accessToken) {
      signinRedirect();
      return;
    }

    setOrderState({ status: "submitting" });

    const orderItems = items.map(({ product, quantity }) => ({
      productId: product.id,
      quantity,
    }));

    try {
      const placedOrder = await placeOrder(accessToken, {
        items: orderItems,
      });

      setOrderState({ status: "placed", order: placedOrder });
      onOrderPlaced();
    } catch (err) {
      console.error(err);
      setOrderState({ status: "failed" });
    }
  };

  let placeOrderBtn;
  if (isLoading) {
    placeOrderBtn = (
      <button type="button" disabled>
        Place order
      </button>
    );
  } else if (isAuthenticated) {
    placeOrderBtn = (
      <button
        type="button"
        onClick={handlePlaceOrder}
        disabled={orderState.status === "submitting"}
      >
        {orderState.status === "submitting"
          ? "Placing order..."
          : "Place order"}
      </button>
    );
  } else {
    placeOrderBtn = (
      <button type="button" onClick={() => signinRedirect()}>
        Log in to place an order
      </button>
    );
  }

  let cartContents;
  if (items.length > 0) {
    cartContents = (
      <div className={styles.panel}>
        <ul className={styles.items}>
          {items.map(({ product, quantity }) => (
            <li key={product.id} className={styles.item}>
              <span className={styles.itemName}>{product.name}</span>
              <span>×{quantity}</span>
              <span>{formatPrice(product.price * quantity)}</span>
            </li>
          ))}
        </ul>
        <p className={styles.total}>
          Estimated total: {formatPrice(calculateEstimatedTotal(items))}
        </p>
        {placeOrderBtn}
      </div>
    );
  } else {
    cartContents = <p className={styles.panel}>Your cart is empty.</p>;
  }

  return (
    <>
      {orderState.status === "placed" && (
        <OrderConfirmation order={orderState.order} key={orderState.order.id} />
      )}
      {cartContents}
      {orderState.status === "failed" && (
        <p role="alert" className={styles.error}>
          Couldn't place your order. Please try again.
        </p>
      )}
    </>
  );
}
