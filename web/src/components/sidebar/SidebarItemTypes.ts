export interface SidebarItem {
  label: string;
  icon: React.ElementType;
  to?: string;
  action?: () => void;
  children?: SidebarItem[];
}
