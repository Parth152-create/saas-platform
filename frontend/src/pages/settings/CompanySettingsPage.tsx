import React, { useState } from 'react';
import { Building2, Save } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from '../../components/common/Card';

export const CompanySettingsPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [companyName, setCompanyName] = useState('Acme Corporation');
  const [legalName, setLegalName] = useState('Acme Technologies Inc.');
  const [timezone, setTimezone] = useState('America/Los_Angeles');
  const [currency, setCurrency] = useState('USD');
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setTimeout(() => {
      setIsSaving(false);
      showToast('success', 'Settings Saved', 'Company profile details updated.');
    }, 600);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Building2 className="w-5 h-5 text-zinc-900 dark:text-neutral-100" />
          <span>Organization & Company Profile</span>
        </CardTitle>
        <p className="text-xs text-zinc-500">
          General organization details for tenant{' '}
          <strong className="text-zinc-800 dark:text-neutral-200">{user?.tenantId}</strong>
        </p>
      </CardHeader>
      <form onSubmit={handleSubmit}>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Organization Display Name"
              value={companyName}
              onChange={(e) => setCompanyName(e.target.value)}
              required
            />
            <Input
              label="Legal Entity Name"
              value={legalName}
              onChange={(e) => setLegalName(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="PostgreSQL Tenant Identifier"
              value={user?.tenantId || ''}
              disabled
              helperText="Managed by backend schema provisioning"
            />
            <Input
              label="Primary Admin Contact"
              value={user?.email || ''}
              disabled
              helperText="Assigned upon initial tenant signup"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Select
              label="Default Workspace Timezone"
              value={timezone}
              onChange={(e) => setTimezone(e.target.value)}
            >
              <option value="America/Los_Angeles">Pacific Time (US & Canada)</option>
              <option value="America/Denver">Mountain Time (US & Canada)</option>
              <option value="America/Chicago">Central Time (US & Canada)</option>
              <option value="America/New_York">Eastern Time (US & Canada)</option>
              <option value="UTC">Coordinated Universal Time (UTC)</option>
              <option value="Europe/London">London (GMT)</option>
            </Select>

            <Select
              label="Base Currency"
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
            >
              <option value="USD">USD ($ - US Dollar)</option>
              <option value="EUR">EUR (€ - Euro)</option>
              <option value="GBP">GBP (£ - British Pound)</option>
            </Select>
          </div>
        </CardContent>

        <CardFooter className="flex justify-end">
          <Button type="submit" isLoading={isSaving} leftIcon={<Save className="w-4 h-4" />}>
            Save Changes
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};
