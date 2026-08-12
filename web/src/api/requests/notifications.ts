import { apiDelete } from "../client";

export function dismissNotification(notificationId: string) {
    return apiDelete(`/v1/notification/${notificationId}`)
}

export function dismissNotifications() {
    return apiDelete(`/v1/notification/dismiss-all`)
}