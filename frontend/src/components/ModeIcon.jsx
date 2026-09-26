import { Plane, Train, Bus, Hotel } from 'lucide-react';

const ICONS = { FLIGHT: Plane, TRAIN: Train, BUS: Bus, HOTEL: Hotel, RESORT: Hotel };

export default function ModeIcon({ mode, size = 18 }) {
  const Icon = ICONS[String(mode).toUpperCase()] || Plane;
  return <Icon size={size} aria-hidden="true" />;
}
