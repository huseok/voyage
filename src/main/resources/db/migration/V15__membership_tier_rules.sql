-- 会员档位规则（可后台配置）：按累计已支付金额门槛匹配最高适用档位，NONE 为隐式默认（无匹配行时）。
create table if not exists t_membership_tier_rules (
  id bigserial primary key,
  tier_code varchar(32) not null,
  min_lifetime_paid_usd numeric(14,2) not null,
  discount_percent int not null,
  sort_no int not null default 0,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint uk_membership_tier_rules_code unique (tier_code)
);

create index if not exists ix_membership_tier_rules_active_min
  on t_membership_tier_rules (is_active, min_lifetime_paid_usd desc);

insert into t_membership_tier_rules (tier_code, min_lifetime_paid_usd, discount_percent, sort_no) values
  ('PLATINUM', 50000, 8, 40),
  ('GOLD', 10000, 5, 30),
  ('SILVER', 5000, 4, 20),
  ('BRONZE', 1000, 2, 10)
on conflict (tier_code) do nothing;
