import type { OrderResponse } from "./api";
import { formatPrice } from "../format";
import styles from "./OrderConfirmation.module.css";
import { textForStatus, useOrderPolling } from "./useOrderPolling";

export type OrderConfirmationProps = {
  order: OrderResponse;
};

export function OrderConfirmation({ order }: OrderConfirmationProps) {
  const currentOrder = useOrderPolling(order);

  return (
    <div
      role="status"
      className={styles.confirmation}
      data-status={currentOrder.status}
    >
      <p>Order #{currentOrder.id} placed.</p>
      <p>Status: {textForStatus[currentOrder.status]}</p>
      <p>Total: {formatPrice(currentOrder.total)}</p>
    </div>
  );
}
