import React from 'react';
import { Eye, EyeOff, Layers, Save, X } from 'lucide-react';
import { Button } from '../common/Button';
import type { WidgetConfig } from './widgetDefaults';

export interface WidgetLibraryDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  widgets: WidgetConfig[];
  onToggleWidget: (widgetId: string) => void;
  onSaveLayout: () => void;
  onResetLayout: () => void;
}

export const WidgetLibraryDrawer: React.FC<WidgetLibraryDrawerProps> = ({
  isOpen,
  onClose,
  widgets,
  onToggleWidget,
  onSaveLayout,
  onResetLayout,
}) => {
  if (!isOpen) return null;

  const categories: ('Metrics' | 'Charts' | 'Lists')[] = ['Metrics', 'Charts', 'Lists'];

  return (
    <div className="fixed inset-0 z-50 overflow-hidden" role="dialog" aria-modal="true">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity"
        onClick={onClose}
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white dark:bg-[#141414] shadow-2xl border-l border-neutral-200 dark:border-[#262626] flex flex-col justify-between animate-in slide-in-from-right duration-300">
          {/* Header */}
          <div>
            <div className="p-6 border-b border-neutral-100 dark:border-[#262626] flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-lg bg-neutral-100 text-neutral-900 dark:bg-[#1f1f1f] dark:text-neutral-100">
                  <Layers className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-neutral-900 dark:text-neutral-100">
                    Widget Library
                  </h3>
                  <p className="text-xs text-neutral-500 dark:text-neutral-400">
                    Customize your dashboard viewport
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="p-1.5 rounded-lg text-neutral-400 hover:bg-neutral-100 dark:hover:bg-[#1f1f1f] cursor-pointer"
                aria-label="Close drawer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Content List */}
            <div className="p-6 space-y-6 max-h-[calc(100vh-200px)] overflow-y-auto">
              <div className="p-3 bg-neutral-50 dark:bg-[#1c1c1c] rounded-lg text-[11px] text-neutral-500 dark:text-neutral-400">
                Changes are saved to your browser session storage. Turn widgets on or off to organize your daily operational dashboard.
              </div>

              {categories.map((cat) => {
                const catWidgets = widgets.filter((w) => w.category === cat);
                if (catWidgets.length === 0) return null;

                return (
                  <div key={cat} className="space-y-3">
                    <h4 className="text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:text-neutral-500">
                      {cat}
                    </h4>
                    <div className="space-y-2">
                      {catWidgets.map((w) => {
                        const Icon = w.icon;
                        return (
                          <div
                            key={w.id}
                            className={`flex items-center justify-between p-3 rounded-xl border transition-all ${
                              w.visible
                                ? 'border-neutral-300 bg-neutral-100/60 dark:border-[#333333] dark:bg-[#1f1f1f]'
                                : 'border-neutral-200 bg-neutral-50/50 dark:border-[#262626] dark:bg-[#141414] opacity-70'
                            }`}
                          >
                            <div className="flex items-start gap-3 min-w-0 pr-3">
                              <div
                                className={`p-2 rounded-lg shrink-0 ${
                                  w.visible
                                    ? 'bg-neutral-950 text-white dark:bg-white dark:text-[#0a0a0a]'
                                    : 'bg-neutral-200 text-neutral-600 dark:bg-[#262626] dark:text-neutral-400'
                                }`}
                              >
                                <Icon className="w-4 h-4" />
                              </div>
                              <div className="min-w-0">
                                <p className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 truncate">
                                  {w.name}
                                </p>
                                <p className="text-[11px] text-neutral-500 dark:text-neutral-400 line-clamp-2 mt-0.5">
                                  {w.description}
                                </p>
                              </div>
                            </div>
                            <Button
                              variant={w.visible ? 'outline' : 'secondary'}
                              size="sm"
                              onClick={() => onToggleWidget(w.id)}
                              leftIcon={
                                w.visible ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />
                              }
                            >
                              {w.visible ? 'Hide' : 'Show'}
                            </Button>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Footer actions */}
          <div className="p-4 px-6 border-t border-neutral-100 dark:border-[#262626] bg-neutral-50 dark:bg-[#0a0a0a] flex items-center justify-between">
            <Button variant="ghost" size="sm" onClick={onResetLayout}>
              Reset Defaults
            </Button>
            <div className="flex items-center gap-2">
              <Button variant="secondary" size="sm" onClick={onClose}>
                Close
              </Button>
              <Button
                size="sm"
                onClick={() => {
                  onSaveLayout();
                  onClose();
                }}
                leftIcon={<Save className="w-3.5 h-3.5" />}
              >
                Save Layout
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
