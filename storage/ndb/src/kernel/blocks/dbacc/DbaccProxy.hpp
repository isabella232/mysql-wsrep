/* Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; version 2 of the License.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

#ifndef NDB_DBACC_PROXY_HPP
#define NDB_DBACC_PROXY_HPP

#include <LocalProxy.hpp>
#include <signaldata/DropTab.hpp>

#define JAM_FILE_ID 343


class DbaccProxy : public LocalProxy {
public:
  DbaccProxy(Block_context& ctx);
  virtual ~DbaccProxy();
  BLOCK_DEFINES(DbaccProxy);

protected:
  virtual SimulatedBlock* newWorker(Uint32 instanceNo);
};


#undef JAM_FILE_ID

#endif