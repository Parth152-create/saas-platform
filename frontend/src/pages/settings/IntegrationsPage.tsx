import { CreditCard, Database, Key, Layers, ShieldCheck } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { Button } from '../../components/common/Button';
import { useToast } from '../../context/ToastContext';

export const IntegrationsPage: React.FC = () => {
  const { showToast } = useToast();

  const integrations = [
    {
      name: 'Stripe Billing & Subscriptions',
      category: 'Payments',
      description: 'Webhook verification, Stripe Checkout sessions, and Customer Portal synchronization.',
      status: 'CONNECTED',
      icon: CreditCard,
      color: 'text-neutral-900 bg-neutral-100 dark:bg-[#1f1f1f] dark:text-neutral-100',
    },
    {
      name: 'Google OAuth & OIDC',
      category: 'Authentication',
      description: 'Invite-only Single Sign-On and Google ID token validation.',
      status: 'CONFIGURED',
      icon: Key,
      color: 'text-neutral-900 bg-neutral-100 dark:bg-[#1f1f1f] dark:text-neutral-100',
    },
    {
      name: 'PostgreSQL Multi-Tenancy',
      category: 'Data Storage',
      description: 'Dynamic schema-per-tenant isolation with per-tenant Flyway database migrations.',
      status: 'ACTIVE',
      icon: Database,
      color: 'text-neutral-900 bg-neutral-100 dark:bg-[#1f1f1f] dark:text-neutral-100',
    },
    {
      name: 'Redis Token Rotation',
      category: 'Caching & Security',
      description: 'Single-use refresh token store and fast revocation index.',
      status: 'CONNECTED',
      icon: ShieldCheck,
      color: 'text-neutral-900 bg-neutral-100 dark:bg-[#1f1f1f] dark:text-neutral-100',
    },
  ];

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Layers className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />
            <span>Platform Services & Integrations</span>
          </CardTitle>
          <p className="text-xs text-neutral-500 mt-1">
            Status of backend micro-integrations and third-party SaaS services
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {integrations.map((item) => {
              const Icon = item.icon;
              return (
                <div
                  key={item.name}
                  className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#141414] flex flex-col justify-between space-y-4"
                >
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <div className={`p-2.5 rounded-xl ${item.color}`}>
                        <Icon className="w-5 h-5" />
                      </div>
                      <Badge variant="success" size="sm" withDot>
                        {item.status}
                      </Badge>
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                        {item.name}
                      </h4>
                      <p className="text-[11px] text-neutral-500 mt-1 leading-relaxed">
                        {item.description}
                      </p>
                    </div>
                  </div>

                  <div className="pt-3 border-t border-neutral-100 dark:border-[#262626] flex justify-between items-center">
                    <span className="text-[10px] text-neutral-400 font-mono">{item.category}</span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => showToast('info', item.name, 'Integration status operational')}
                    >
                      Inspect Status
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
