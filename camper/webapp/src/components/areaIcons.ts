import { BookmarkIcon, CalendarIcon, ListBulletIcon, PersonIcon } from '@radix-ui/react-icons';
import type { Area } from '../lib/areas';

/**
 * One icon per area, used everywhere the area is named: its tab, the chip
 * beside a root screen's title, and the eyebrow above a detail screen's
 * title. Seeing the same icon in the tab bar and at the top of the screen is
 * what ties a screen to its tab.
 */
export const AREA_ICON: Record<Area, typeof BookmarkIcon> = {
  recipes: BookmarkIcon,
  plans: CalendarIcon,
  shopping: ListBulletIcon,
  account: PersonIcon,
};
