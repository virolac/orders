import type { OrderResponse } from "./api";
import { textForStatus, useOrderPolling } from "./useOrderPolling";
import styles from "./OrderRow.module.css";
import { formatDate, formatPrice } from "../format";

export type OrderRowProps = {
  order: OrderResponse;
};

export function OrderRow({ order }: OrderRowProps) {
  const currentOrder = useOrderPolling(order);

  return (
    <li className={styles.row} data-status={currentOrder.status}>
      <span className={styles.id}>#{currentOrder.id}</span>
      <span className={styles.date}>{formatDate(currentOrder.createdAt)}</span>
      <span className={styles.total}>{formatPrice(currentOrder.total)}</span>
      <span className={styles.status}>
        {textForStatus[currentOrder.status]}
      </span>
    </li>
  );
}
