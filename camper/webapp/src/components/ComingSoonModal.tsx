import { Button } from './ui/Button';
import './Modal.css';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  icon: React.ReactNode;
}

const FLAVOR_TEXTS: Record<string, string[]> = {
  equipment: [
    "The supply wagon is still on its way...",
    "Our quartermaster is gathering provisions.",
    "The gear chest remains locked — for now.",
  ],
  kitchen: [
    "The camp cook hasn't arrived yet...",
    "Pots and pans are still packed away.",
    "The hearth awaits its first meal.",
  ],
  itinerary: [
    "The cartographer is still charting the route...",
    "The map's ink hasn't dried yet.",
    "Our path through the wilderness is being drawn.",
  ],
  tent: [
    "Tents are still rolled up in the wagon...",
    "Shelter assignments await the full party.",
    "The sleeping quarters are being prepared.",
  ],
};

function getFlavorText(category: string): string {
  const texts = FLAVOR_TEXTS[category] || FLAVOR_TEXTS.equipment;
  return texts[Math.floor(Math.random() * texts.length)];
}

export function ComingSoonModal({ isOpen, onClose, title, icon }: Props) {
  if (!isOpen) return null;

  const category = title.toLowerCase().includes('equip') ? 'equipment'
    : title.toLowerCase().includes('menu') || title.toLowerCase().includes('kitchen') ? 'kitchen'
    : title.toLowerCase().includes('itinerary') || title.toLowerCase().includes('map') ? 'itinerary'
    : 'tent';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content modal-content--coming-soon" onClick={e => e.stopPropagation()}>
        <div className="modal-icon-large">{icon}</div>
        <h2 className="modal-title">{title}</h2>
        <p className="modal-flavor">{getFlavorText(category)}</p>
        <div className="modal-divider">
          <svg width="120" height="12" viewBox="0 0 120 12">
            <path d="M0,6 Q30,0 60,6 Q90,12 120,6" fill="none" stroke="var(--tan-deep)" strokeWidth="1.5" />
          </svg>
        </div>
        <p className="modal-subtitle">This part of camp is still being set up. Check back soon, adventurer.</p>
        <Button onClick={onClose}>
          Return to Camp
        </Button>
      </div>
    </div>
  );
}
